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

package de.akquinet.tim.registrationservice.api.keycloak

import com.fasterxml.jackson.databind.ObjectMapper
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceCreateService.Companion.ERROR_LOG_TEMPLATE
import de.akquinet.tim.registrationservice.config.KeycloakAdminConfig
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import jakarta.ws.rs.WebApplicationException
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.RealmRepresentation
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class KeycloakRealmService @Autowired constructor(
    private val logger: Logger,
    @Qualifier("master") private val keycloak: Keycloak,
    private val keycloakProperties: KeycloakAdminConfig.Properties,
    @Value("classpath:realm-template.json") val realmTemplate: Resource
) {

    fun createRealmKeycloakAndGetSecret(
        messengerInstanceEntity: MessengerInstanceEntity
    ): String? =
        try {
            val mapper = ObjectMapper()
            val realm: RealmRepresentation = mapper.readValue(
                realmTemplate.inputStream,
                RealmRepresentation::class.java
            )
            // set realm name
            realm.realm = messengerInstanceEntity.instanceId
            // set synapse url
            realm.clients.find { clientRepresentation -> clientRepresentation.clientId == "synapse" }?.rootUrl =
                "https://${messengerInstanceEntity.publicBaseUrl}"
            // configure smtp server
            realm.smtpServer["from"] = keycloakProperties.smtp.from
            realm.smtpServer["fromDisplayName"] = keycloakProperties.smtp.fromDisplayName
            realm.smtpServer["password"] = keycloakProperties.smtp.password

            keycloak.realms().create(realm)
            // refresh token to get new roles for the new realm
            keycloak.tokenManager().refreshToken()
            // get generated client-secret from keycloak
            keycloak.realm(messengerInstanceEntity.instanceId).clients().findByClientId("synapse").last().secret
        } catch (e: Exception) {
            when (e) {
                is WebApplicationException ->
                    logger.error(
                        "$ERROR_LOG_TEMPLATE, status: {}, message: {}",
                        messengerInstanceEntity.serverName,
                        "keycloak realm",
                        e.response.status,
                        e.response.readEntity(String::class.java),
                        e
                    )

                else -> logger.error(ERROR_LOG_TEMPLATE, messengerInstanceEntity.serverName, "keycloak realm", e)
            }
            null
        }

    fun deleteRealmKeycloak(realmName: String): KeycloakOperationResult {
        return try {
            keycloak.realm(realmName).remove()
            KeycloakOperationResult.OK_REALM_DELETED
        } catch (e: WebApplicationException) {
            logger.error(
                "Error deleting keycloak realm, status: {}, message: {}",
                e.response.status,
                e.response.readEntity(String::class.java)
            )

            KeycloakOperationResult.FAILED_REALM_DELETED
        }
    }
}
