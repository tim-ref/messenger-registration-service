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

package de.akquinet.tim.registrationservice.rawdata

import de.akquinet.tim.registrationservice.api.messengerservice.RawDataServiceConfig
import de.akquinet.tim.registrationservice.rawdata.model.Operation
import de.akquinet.tim.registrationservice.rawdata.model.PerformanceData
import de.akquinet.tim.registrationservice.rawdata.model.RawData
import de.akquinet.tim.registrationservice.rawdata.model.RawDataTask
import de.akquinet.tim.registrationservice.util.UserService
import org.springframework.http.HttpStatusCode
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Service
class RawDataService(
    private val rawDataServiceConfig: RawDataServiceConfig,
    private val restTemplate: RestTemplate,
    private val userService: UserService,
    private val scheduler: ThreadPoolTaskScheduler
) {

    // this "cache" is needed because we cannot get the response body size in RawDataCreationFilter
    var responseBodySize: Int = 0

    fun collectAndSendRawData(
        requestContentLength: Int,
        responseBodySize: Int,
        responseStatus: HttpStatusCode,
        elapsed: Duration,
        operation: Operation,
        matrixDomain: String,
        telematikId: String? = null,
        professionOid: String? = null,
        userId: String? = null
    ): PerformanceData {


        val rawData = RawData(
            `Inst-ID` = userId ?: userService.getUserIdFromContextSafely(),
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
            tID = telematikId ?: userService.loadUserAttributeByClaim("telematik_id"),
            profOID = professionOid ?: userService.loadUserAttributeByClaim("profession_oid"),
            Res = responseStatus.value().toString()
        )

        return PerformanceData(
            start = Instant.now(),
            durationInMs = elapsed.toInt(DurationUnit.MILLISECONDS),
            operation = operation,
            status = responseStatus.value().toString(),
            message = rawData
        ).also { sendMessageLog(it) }
    }

    fun sendMessageLog(performanceData: PerformanceData) {
        scheduler.schedule(
            RawDataTask(performanceData, restTemplate, rawDataServiceConfig),
            Instant.now()
        )
    }
}
