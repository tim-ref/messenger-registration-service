/*
 * Copyright (C) 2023-2024 akquinet GmbH
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

package de.akquinet.tim.registrationservice.config
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "vzd")
data class VZDConfig(
    val serviceUrl: String,
    val tokenUrl: String,
    val tokenPath: String,
    val authenticationPath: String,
    val healthPath: String,
    val federationListPath: String,
    val federationCheckPath: String,
    val userWhereIsPath: String,
    val checkRevocationStatus: Boolean,
    val addDomainPath: String,
    val deleteDomainPath: String,
    val clientId: String,
    val clientSecret: String,
    val trustStorePath: String,
    val trustStorePassword: String,
)
