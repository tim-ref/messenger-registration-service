/*
 * Copyright (C) 2023-2024 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.registrationservice.rawdata.model

import de.akquinet.tim.registrationservice.api.messengerservice.RawDataServiceConfig
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.lang.invoke.MethodHandles


class RawDataTask(
    private val performanceData: PerformanceData,
    private val restTemplate: RestTemplate,
    private val rawDataServiceConfig: RawDataServiceConfig
): Runnable {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    override fun run() {
        try {
            HttpEntity<PerformanceData>(performanceData, basicHeaders()).also {
                restTemplate.postForObject(
                    "${rawDataServiceConfig.protocol}://${rawDataServiceConfig.host}:${rawDataServiceConfig.port}${rawDataServiceConfig.path}",
                    it,
                    String::class.java
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to send raw data {}", e.message, e)
        }
    }

    private fun basicHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }
}
