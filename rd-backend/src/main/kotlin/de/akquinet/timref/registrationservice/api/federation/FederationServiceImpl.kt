/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.akquinet.timref.registrationservice.api.federation

import com.google.gson.Gson
import de.akquinet.timref.registrationservice.api.federation.model.AddDomainResponse
import de.akquinet.timref.registrationservice.api.federation.model.DeleteDomainResponse
import de.akquinet.timref.registrationservice.api.federation.model.Domain
import de.akquinet.timref.registrationservice.api.federation.model.FederationList
import de.akquinet.timref.registrationservice.api.federation.model.FederationListHeader
import de.akquinet.timref.registrationservice.config.VZDConfig
import de.akquinet.timref.registrationservice.persistance.federation.FederationRepository
import de.akquinet.timref.registrationservice.rawdata.RawDataService
import de.akquinet.timref.registrationservice.rawdata.model.Operation
import de.akquinet.timref.registrationservice.security.signature.CertPathValidationResult
import de.akquinet.timref.registrationservice.security.signature.JwsSignatureVerificationResult
import de.akquinet.timref.registrationservice.security.signature.JwsVerificationResult
import de.akquinet.timref.registrationservice.security.signature.SignatureService
import de.akquinet.timref.registrationservice.security.toX509Certificate
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.event.Level
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


data class Token(
    val access_token: String,
    val token_type: String,
    val expires_in: Long
)

internal class VzdConnectionException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)
internal class AuthenticationTokenException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

@EnableScheduling
@Service
class FederationServiceImpl(
    private val logger: Logger,
    val federationRepository: FederationRepository,
    val vzdConfig: VZDConfig,
    val signatureService: SignatureService,
    val rawdataService: RawDataService
) : FederationService {

    private val gson = Gson()

    fun getVerifiedFederationListFromRemote(response: HttpURLConnection): JwsVerificationResult {

        val jwsString = BufferedInputStream(response.inputStream).bufferedReader().use { it.readText() }
        val decoder = Base64.getUrlDecoder()
        val header = jwsString.split(".")[0]
        val decodedHeader = String(decoder.decode(header))

        val certPath = gson.fromJson(decodedHeader, FederationListHeader::class.java)
            .toFederationListBase64Header().x5c.map { it.toX509Certificate() }

        val jwsSignatureVerificationResult =
            signatureService.parseAndVerifySignature(jwsString, certPath.first().publicKey)

        if (jwsSignatureVerificationResult is JwsSignatureVerificationResult.Valid) {
            val certPathIsValid = signatureService.verifyCertificatePath(
                certPath,
                vzdConfig.checkRevocationStatus
            ) is CertPathValidationResult.Valid

            if (certPathIsValid) {
                val federationListJson =
                    gson.fromJson(jwsSignatureVerificationResult.jws.payload, FederationList::class.java)

                return JwsVerificationResult.Valid(federationListJson)
            }
        }

        return JwsVerificationResult.Invalid
    }

    fun getVZDBearerToken(): Token {
        val client = OkHttpClient()

        val uri = vzdConfig.tokenUrl + vzdConfig.tokenPath
        val builder = uri.toHttpUrlOrNull()?.newBuilder()

        val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
        val body =
            "client_id=${vzdConfig.clientId}&client_secret=${vzdConfig.clientSecret}&grant_type=client_credentials".toRequestBody(
                mediaType
            )
        val request = Request.Builder()
            .url(builder?.build().toString())
            .post(body)
            .build()


        val response = client.newCall(request).execute()
        if (response.code != 200) {
            throw VzdConnectionException("Unable to get bearer token at vzd. Server Response:  ${response.code}")
        }
        val byteStream = response.body?.byteStream()
        val responseBody =
            if (byteStream != null) BufferedInputStream(byteStream).bufferedReader().use { it.readText() } else ""
        return gson.fromJson(responseBody, Token::class.java)
    }

    fun authenticateAtVZD(bearerToken: Token): Token {

        val uri = vzdConfig.serviceUrl + vzdConfig.authenticationPath

        val connection: HttpURLConnection = setConnectionParameters(bearerToken, uri)
        if (connection.responseCode != 200) {
            throw VzdConnectionException("Could not authenticate with VZD, unexpected response code ${connection.responseCode}")
        }

        val responseBody = BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
        return gson.fromJson(responseBody, Token::class.java)
    }

    fun connectToVzd(uri: String, queryParameters: Map<String, Any>?): HttpURLConnection {

        val bearerToken = getVZDBearerToken()
        val authenticationToken = authenticateAtVZD(bearerToken)
        val connection: HttpURLConnection = setConnectionParameters(authenticationToken, uri, queryParameters)
        connection.connect()
        return connection

    }

    private fun setConnectionParameters(
        authenticationToken: Token,
        uri: String,
        queryParameters: Map<String, Any>? = null
    ): HttpURLConnection {

        if (authenticationToken.token_type.isBlank() || authenticationToken.access_token.isBlank()) {
            throw AuthenticationTokenException("Unable to set connection parameters: the authentication token is empty")
        }

        val builder = UriComponentsBuilder.fromHttpUrl(uri)
        queryParameters?.forEach { (key, value) -> builder.queryParam(key, value) }
        val url = URL(builder.toUriString())
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.readTimeout = 120000
        connection.connectTimeout = 12000
        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "Authorization",
            capitalizeFirstChar(authenticationToken.token_type).plus(" ").plus(authenticationToken.access_token)
        )

        return connection

    }

    fun saveFederationListToRepository(federationList: FederationList) {
        federationRepository.deleteAllByVersionNot(federationList.version)
        if (federationRepository.findAll().isEmpty()) {
            federationRepository.save(federationList.toEntity())
        }
    }

    private fun createDomainAddSuccessResponse(responseBody: String): AddDomainResponse =
        AddDomainResponse(
            httpStatus = HttpStatus.OK,
            errorMessage = "No error",
            domain = gson.fromJson(responseBody, Domain::class.java)
        )

    fun addDomainToVzd(domain: Domain): Response {
        val bearerToken = getVZDBearerToken()
        val authenticationToken = authenticateAtVZD(bearerToken)

        val client = OkHttpClient()

        val uri = vzdConfig.serviceUrl + vzdConfig.addDomainPath
        val builder = uri.toHttpUrlOrNull()?.newBuilder()

        val mediaType = "application/json".toMediaTypeOrNull()
        val body = gson.toJson(domain).toRequestBody(mediaType)
        val request = Request.Builder()
            .url(builder?.build().toString())
            .post(body)
            .addHeader(
                "Authorization",
                capitalizeFirstChar(authenticationToken.token_type).plus(" ").plus(authenticationToken.access_token)
            )
            .build()

        return client.newCall(request).execute()
    }

    fun deleteDomainFromVzd(domain: String): Response {
        val bearerToken = getVZDBearerToken()
        val authenticationToken = authenticateAtVZD(bearerToken)

        val client = OkHttpClient()

        val uri = vzdConfig.serviceUrl + vzdConfig.deleteDomainPath + domain
        val builder = uri.toHttpUrlOrNull()?.newBuilder()

        val request = Request.Builder()
            .url(builder?.build().toString())
            .delete()
            .addHeader(
                "Authorization",
                capitalizeFirstChar(authenticationToken.token_type).plus(" ").plus(authenticationToken.access_token)
            )
            .build()

        return client.newCall(request).execute()
    }

    @Scheduled(cron = "0 */5 * * * *")
    override fun getFederationListResponse() {
        val uri = vzdConfig.serviceUrl + vzdConfig.federationListPath
        val connection: HttpURLConnection = if (federationRepository.findAll().isEmpty()) {
            connectToVzd(uri, null)
        } else {
            val paramMap = mutableMapOf<String, Any>()
            paramMap["version"] = federationRepository.findAll().first().version
            connectToVzd(uri, paramMap)
        }

        if (connection.responseCode == 200) {
            val jwsVerificationResult = getVerifiedFederationListFromRemote(connection)
            if (jwsVerificationResult is JwsVerificationResult.Valid) {
                logger.info("Starting Federation list Save")
                saveFederationListToRepository(jwsVerificationResult.federationList)
                logger.info("Federation list successfully updated")
            } else {
                logger.error("Federation list update: Signature or certificate path is invalid")
            }
        } else if (connection.responseCode == 204) {
            logger.info("Federation list update: no new list version")
        } else {
            logger.info("Federation list update: error response from vzd")
        }
    }

    @ExperimentalTime
    override fun updateVZDFederationList(domain: Domain): AddDomainResponse {
        val requestHeaderContentLength: Int
        val vzdResponse: AddDomainResponse
        val (responseEntity, elapsed) = measureTimedValue {

            val response = addDomainToVzd(domain)
            requestHeaderContentLength = response.request.body?.contentLength()?.toInt() ?: 0
            val responseBodyString = response.body?.string()

            val domainResponse = when (response.code) {
                200 -> {
                    if (!responseBodyString.isNullOrBlank()) {
                        createDomainAddSuccessResponse(responseBodyString)
                    } else {
                        throw IllegalStateException("Domain in vzd response is empty.")
                    }
                }

                400 -> AddDomainResponse(
                    httpStatus = HttpStatus.BAD_REQUEST,
                    errorMessage = "VZD scheme error"
                )

                401 -> AddDomainResponse(
                    httpStatus = HttpStatus.UNAUTHORIZED,
                    errorMessage = "No VZD authorization"
                )

                403 -> AddDomainResponse(
                    httpStatus = HttpStatus.FORBIDDEN,
                    errorMessage = "VZD access forbidden"
                )

                409 -> AddDomainResponse(
                    httpStatus = HttpStatus.CONFLICT,
                    errorMessage = "Domain already exists"
                )

                500 -> AddDomainResponse(
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    errorMessage = "internal server error at VZD"
                )

                else -> AddDomainResponse(
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    errorMessage = "unknown response code"
                )
            }

            val logLevel = if (domainResponse.httpStatus.is2xxSuccessful) Level.DEBUG else Level.WARN
            
            logger.atLevel(logLevel).log("VZD response body: {}", responseBodyString)
            logger.atLevel(logLevel).log("VZD response message when creating: {}", domainResponse.errorMessage)
            logger.atLevel(logLevel).log("VZD statuscode when creating: {}", response.code)

            vzdResponse = domainResponse
            domainResponse.toResponseEntity(gson)
        }

        rawdataService.collectAndSendRawData(
            requestHeaderContentLength,
            responseEntity.body!!.length,
            responseEntity.statusCode,
            elapsed,
            Operation.RS_ADD_MESSENGER_SERVICE_TO_FEDERATION,
            domain.domain
        )
        return vzdResponse
    }

    override fun deleteDomainFromVZDFederationList(domain: String): DeleteDomainResponse {
        val response = deleteDomainFromVzd(domain)
        val responseBodyString = response.body?.string()

        val domainResponse = when (response.code) {
            204 -> DeleteDomainResponse(
                httpStatus = HttpStatus.NO_CONTENT
            )

            400 -> DeleteDomainResponse(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorMessage = "VZD scheme error"
            )

            401 -> DeleteDomainResponse(
                httpStatus = HttpStatus.UNAUTHORIZED,
                errorMessage = "No VZD authorization"
            )

            403 -> DeleteDomainResponse(
                httpStatus = HttpStatus.FORBIDDEN,
                errorMessage = "VZD access forbidden"
            )

            404 -> DeleteDomainResponse(
                httpStatus = HttpStatus.NOT_FOUND,
                errorMessage = "VZD endpoint not found"
            )

            500 -> DeleteDomainResponse(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                errorMessage = "internal server error at VZD"
            )

            else -> DeleteDomainResponse(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                errorMessage = "unknown response code"
            )
        }

        val logLevel = if (domainResponse.httpStatus.is2xxSuccessful) Level.DEBUG else Level.WARN

        logger.atLevel(logLevel).log("VZD response message when deleting: {}", response.message)
        logger.atLevel(logLevel).log("VZD statuscode when deleting: {}", response.code)
        logger.atLevel(logLevel).log("VZD response body: {}", responseBodyString)

        return domainResponse
    }

    private fun capitalizeFirstChar(input: String): String =
        input.replaceFirstChar { it.uppercase() }
}
