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

package de.akquinet.tim.registrationservice.api.keycloak

import com.fasterxml.jackson.databind.ObjectMapper
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceService.Companion.ERROR_LOG_TEMPLATE
import de.akquinet.tim.registrationservice.config.KeycloakAdminConfig
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateRealmRequest
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import jakarta.ws.rs.WebApplicationException
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.RealmRepresentation
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class KeycloakRealmService @Autowired constructor(
    private val logger: Logger,
    @Qualifier("master") private val keycloak: Keycloak,
    private val keycloakProperties: KeycloakAdminConfig.Properties,
    @Value("classpath:realm-template.json") val realmTemplate: Resource
) {

    fun createRealm(createRealmRequest: CreateRealmRequest): KeycloakOperationResult =
        try {
            val mapper = ObjectMapper()
            val realm: RealmRepresentation = mapper.readValue(
                realmTemplate.inputStream,
                RealmRepresentation::class.java
            )
            val realmPasswordPolicy = """
                length(8) and lowerCase(1) and upperCase(1) and digits(1) and notUsername(undefined) and notEmail(undefined) and hashAlgorithm(pbkdf2-sha512) and passwordHistory(3)
                """.trimIndent()

            // set realm name
            realm.realm = createRealmRequest.realmName
            // set synapse url
            realm.clients
                ?.find { clientRepresentation -> clientRepresentation.clientId == "synapse" }?.rootUrl =
                "https://${createRealmRequest.synapseClientRootUrl}"


            // configure smtp server
            realm.smtpServer["from"] = keycloakProperties.smtp.from
            realm.smtpServer["fromDisplayName"] = keycloakProperties.smtp.fromDisplayName
            realm.smtpServer["password"] = keycloakProperties.smtp.password
            realm.passwordPolicy = realmPasswordPolicy


            keycloak.realms().create(realm)
            KeycloakOperationResult.REALM_CREATED
        } catch (e: WebApplicationException) {
            logger.error(
                "$ERROR_LOG_TEMPLATE, status: {}, message: {}",
                createRealmRequest.realmName,
                "keycloak realm",
                e.response.status,
                e.response.readEntity(String::class.java),
                e
            )
            when (e.response.status) {
                HttpStatus.CONFLICT.value() -> KeycloakOperationResult.REALM_ALREADY_PRESENT
                else -> KeycloakOperationResult.REALM_NOT_CREATED
            }

        }

    fun getSynapseClientSecretForRealm(realmName: String): String? {
        // refresh token to get new roles for the new realm
        keycloak.tokenManager().refreshToken()
        // get generated client-secret from keycloak

        val realm = keycloak.realm(realmName)
        if (realm == null) {
            logger.warn("Keycloak: Realm not found by name $realmName")
        }
        val synapseClient = realm?.clients()?.findByClientId("synapse")?.lastOrNull()
        if (synapseClient == null) {
            logger.warn("Keycloak: Client 'synapse' not found for with name $realmName")
        }
        val secret = synapseClient?.secret
        if (secret == null) {
            logger.warn("Keycloak: Secret not found for client 'synapse' in realm with name $realmName")
        }
        return secret
    }

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
            KeycloakOperationResult.REALM_DELETED
        } catch (e: WebApplicationException) {
            logger.error(
                "Error deleting keycloak realm, status: {}, message: {}",
                e.response.status,
                e.response.readEntity(String::class.java)
            )

            KeycloakOperationResult.REALM_NOT_DELETED
        }
    }
}
