/*
 * Copyright (C) 2023 - 2024 akquinet GmbH
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KeycloakAdminConfig @Autowired constructor(
    val properties: Properties
) {

    @ConfigurationProperties(prefix = "keycloak-admin")
    data class Properties(
        val readinessEndpoint: String,
        val smtp: SMTP,
        val masterRealm: MasterRealm,
        val timRealm: TimRealm
    ) {
        @ConfigurationProperties(prefix = "keycloak-admin.master-realm")
        data class MasterRealm(
            val url: String,
            val realmName: String,
            val clientId: String,
            val clientSecret: String,
        )

        @ConfigurationProperties(prefix = "keycloak-admin.tim-realm")
        data class TimRealm(
            val url: String,
            val realmName: String,
            val clientId: String,
            val clientSecret: String
        )

        @ConfigurationProperties(prefix = "keycloak-admin.smtp")
        data class SMTP(
            val from: String,
            val fromDisplayName: String,
            val password: String
        )
    }

    @Bean
    @Qualifier("master")
    fun masterKeycloakClient(): Keycloak =
        KeycloakBuilder.builder()
            .serverUrl(this.properties.masterRealm.url)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .realm(this.properties.masterRealm.realmName)
            .clientId(this.properties.masterRealm.clientId)
            .clientSecret(this.properties.masterRealm.clientSecret)
            .build()

    @Bean
    @Qualifier("tim")
    fun timKeycloakClient(): Keycloak =
        KeycloakBuilder.builder()
            .serverUrl(this.properties.timRealm.url)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .realm(this.properties.timRealm.realmName)
            .clientId(this.properties.timRealm.clientId)
            .clientSecret(this.properties.timRealm.clientSecret)
            .build()
}