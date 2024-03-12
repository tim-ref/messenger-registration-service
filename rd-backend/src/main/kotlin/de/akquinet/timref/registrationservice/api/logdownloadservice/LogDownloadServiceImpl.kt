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

package de.akquinet.timref.registrationservice.api.logdownloadservice

import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.timref.registrationservice.rawdata.RawDataService
import de.akquinet.timref.registrationservice.util.NotLoggedInException
import de.akquinet.timref.registrationservice.util.TrivialResponseErrorHandler
import de.akquinet.timref.registrationservice.util.UserService
import org.slf4j.Logger
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant

@Service
class LogDownloadServiceImpl(
    private val logger: Logger,
    private val lokiConfig: LogDownloadLokiConfig,
    private val userService: UserService,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val rawDataService: RawDataService
) : LogDownloadService {

    private val restTemplate =
        RestTemplateBuilder()
            .errorHandler(TrivialResponseErrorHandler())
            .build()

    private fun getLokiResponse(
        serverName: String,
        containerName: String,
        start: Long,
        end: Long
    ): ResponseEntity<String> {
        val httpHeaders = HttpHeaders().apply {
            add("X-Scope-OrgID", serverName.replace(".", ""))
        }

        val lokiBaseUrl = lokiConfig.let { "${it.protocol}://${it.host}:${it.port}${it.path}" }

        val lokiUrl = UriComponentsBuilder
            .fromHttpUrl(lokiBaseUrl)
            .queryParam("query", "{container=\"$containerName\"}")
            .queryParam("limit", 10_000)
            .queryParam("start", start)
            .queryParam("end", end)
            .build()
            .encode()
            .toUri()
        logger.info("Loki: $lokiUrl")

        return restTemplate.exchange(
            lokiUrl,
            HttpMethod.GET,
            HttpEntity(null, httpHeaders),
            String::class.java
        )
    }

    override fun getLogs(
        serverName: String,
        containerName: String,
        start: Long?,
        end: Long?
    ): LogDownloadResult {
        try {
            messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
                serverName = serverName,
                userId = userService.getUserIdFromContext()
            ) ?: return LogDownloadResult.Unauthorized(serverName)
        } catch (e: NotLoggedInException) {
            return LogDownloadResult.Unauthorized(serverName)
        }

        val now = Instant.now().epochSecond
        val endOrDefault = end ?: now
        val startOrDefault = start ?: (endOrDefault - 86_400)

        if (startOrDefault >= endOrDefault)
            return LogDownloadResult.InvalidInput("start must be before end")

        if (endOrDefault > now)
            return LogDownloadResult.InvalidInput("end must not be in the future")

        var lokiResponse: ResponseEntity<String>? = null

        try {
            lokiResponse = getLokiResponse(serverName, containerName, startOrDefault, endOrDefault)
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
        }
        finally {
            rawDataService.responseBodySize = lokiResponse?.body?.length?: 0
        }

        val lokiResponseBody = lokiResponse?.body
            ?: return LogDownloadResult.Unexpected("Loki response body was null")

        if (!lokiResponse.statusCode.is2xxSuccessful)
            return LogDownloadResult.Unexpected("Loki response was unsuccessful: ${lokiResponse.body}")

        return lokiResponseBody.toByteArray().let {
            LogDownloadResult.LogStream(
                inputStream = it.inputStream(),
                numberOfBytes = it.size
            )
        }
    }
}
