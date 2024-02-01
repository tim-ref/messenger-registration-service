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

package de.akquinet.timref.registrationservice.availability

import de.akquinet.timref.registrationservice.config.VZDConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
@ConditionalOnEnabledHealthIndicator("vzd")
class VzdHealthIndicator @Autowired constructor(
    @Qualifier("healthIndicator") private val restTemplate: RestTemplate,
    private val vzdConfig: VZDConfig
) : HealthIndicator {

    override fun health(): Health {
        val vzdHealthUrl = vzdConfig.serviceUrl + vzdConfig.healthPath

        return try {
            val response = restTemplate.getForEntity(vzdHealthUrl, String::class.java)
            if (response.statusCode.isSameCodeAs(HttpStatus.OK)) {
                Health.up().build()
            } else {
                Health.down()
                    .withDetail("error", "Could not connect to VZD at $vzdHealthUrl")
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", "Could not connect to VZD at $vzdHealthUrl")
                .withDetail("exception_message", e.message ?: "not set")
                .build()
        }
    }
}
