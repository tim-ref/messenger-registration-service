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

package de.akquinet.tim.registrationservice.config

import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean


@ConfigurationProperties(prefix = "keycloak-admin")
class KeycloakAdminConfig(
    val url: String,
    val readinessEndpoint: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String,
    val smtp: SMTP
) {
    data class SMTP(
        val from: String,
        val fromDisplayName: String,
        val password: String
    )

    @Bean
    fun getKeycloakClient(): Keycloak {
        return KeycloakBuilder.builder()
            .serverUrl(this.url)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .realm(this.realm)
            .clientId(this.clientId)
            .clientSecret(this.clientSecret)
            .build()
    }

}