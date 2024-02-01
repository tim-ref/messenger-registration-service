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

package de.akquinet.timref.registrationservice.rawdata

import com.google.gson.Gson
import de.akquinet.timref.registrationservice.api.messengerservice.RawDataServiceConfig
import de.akquinet.timref.registrationservice.rawdata.model.Operation
import de.akquinet.timref.registrationservice.rawdata.model.RawData
import de.akquinet.timref.registrationservice.rawdata.model.RawDataMetaData
import de.akquinet.timref.registrationservice.util.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Service
class RawDataServiceImpl(
    private val rawDataServiceConfig: RawDataServiceConfig,
    private val restTemplate: RestTemplate,
    private val userService: UserService
) : RawdataService {

    private val logger: Logger = LoggerFactory.getLogger(RawDataServiceImpl::class.java)

    private val gson = Gson()

    // this "cache" is needed because we cannot get the response body size in RawDataCreationFilter
    var responseBodySize: Int = 0

    override fun collectAndSendRawData(
        requestContentLength: Int,
        responseBodySize: Int,
        responseStatus: HttpStatusCode,
        elapsed: Duration,
        operation: Operation,
        matrixDomain: String
    ): RawDataMetaData {
        val rawData = RawData(
            `Inst-ID` = if (userService.getPrincipal() != null) userService.getUserIdFromContext() else "TIM-registration-service",
            `UA-PV` = "n/a",
            `UA-PTV` = "n/a",
            `UA-OS-VERSION` = "n/a",
            `UA-cid` = "n/a",
            `UA-P` = "n/a",
            `UA-OS` = "n/a",
            `UA-A` = "n/a",
            `M-Dom` = matrixDomain,
            sizeIn = requestContentLength,
            sizeOut = responseBodySize,
            tID = userService.loadUserAttributeByClaim("telematik_id") ?: "",
            profOID = userService.loadUserAttributeByClaim("profession_oid") ?: "",
            Res = responseStatus.value().toString()
        )
        return RawDataMetaData(
            start = Instant.now(),
            durationInMs = elapsed.toInt(DurationUnit.MILLISECONDS),
            operation = operation,
            status = responseStatus.value().toString(),
            message = rawData
        ).also { sendMessageLog(it) }
    }

    override fun sendMessageLog(rawDataMetaData: RawDataMetaData): HttpStatus {
        var status = HttpStatus.OK
        return try {
            HttpEntity<RawDataMetaData>(rawDataMetaData, basicHeaders()).also {
                restTemplate.postForObject(
                    "${rawDataServiceConfig.protocol}://${rawDataServiceConfig.host}:${rawDataServiceConfig.port}${rawDataServiceConfig.path}",
                    it,
                    String::class.java
                )
            }
            status
        } catch (ex: Exception) {
            status = HttpStatus.INTERNAL_SERVER_ERROR
            logger.error(ex.message)
            status
        }
    }

    private fun basicHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }

}
