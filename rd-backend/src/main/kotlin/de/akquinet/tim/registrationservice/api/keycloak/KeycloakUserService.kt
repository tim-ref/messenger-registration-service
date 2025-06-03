/*
 * Copyright (C) 2023 - 2025 akquinet GmbH
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

import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceService.Companion.ORG_ADMIN_ERROR_LOG_TEMPLATE
import de.akquinet.tim.registrationservice.api.operator.OperatorService
import de.akquinet.tim.registrationservice.config.KeycloakAdminConfig
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateAdminUser201Response
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateAdminUserRequest
import de.akquinet.tim.registrationservice.openapi.model.operator.GetMessengerInstanceInitialAdminCreds200Response
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.persistance.orgAdmin.model.OrgAdminEntity
import de.akquinet.tim.registrationservice.service.orgadmin.OrgAdminManagementService
import jakarta.ws.rs.WebApplicationException
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class KeycloakUserService @Autowired constructor(
    private val logger: Logger,
    @Qualifier("master") private val keycloakMaster: Keycloak,
    @Qualifier("tim") private val keycloakTim: Keycloak,
    private val keycloakProperties: KeycloakAdminConfig.Properties,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val orgAdminManagementService: OrgAdminManagementService,
    private val operatorService: OperatorService,
) {

    fun createKeycloakAdminUser(
        realmName: String,
        user: GetMessengerInstanceInitialAdminCreds200Response,
        emailAddress: String? = null,
    ): Boolean {
        val password = preparePasswordRepresentation(user.password)
        val userName = prepareUserRepresentation(
            userName = user.username,
            cR = password,
            emailAddress = emailAddress
        )

        return try {
            val realmResource = keycloakMaster.realm(realmName)
            val usersResource = realmResource.users()
            val response = usersResource.create(userName)
            if (response.status == HttpStatus.CREATED.value()) {
                return try {
                    // add client roles to newly created user
                    // the roles cannot be added before or on user creation
                    val userId = CreatedResponseUtil.getCreatedId(response)
                    val createdUserResource = usersResource[userId]

                    val realmManagementClient = realmResource.clients()
                        .findByClientId("realm-management")[0]

                    val manageUsersRole = realmResource.clients()[realmManagementClient.id]
                        .roles()["manage-users"].toRepresentation()

                    val viewUsersRole = realmResource.clients()[realmManagementClient.id]
                        .roles()["view-users"].toRepresentation()

                    createdUserResource.roles()
                        .clientLevel(realmManagementClient.id).add(listOf(manageUsersRole, viewUsersRole))
                    true
                } catch (e: Exception) {
                    logger.error(ORG_ADMIN_ERROR_LOG_TEMPLATE, realmName, "keycloak admin roles", e)
                    deleteKeycloakUser(realmName, user.username)

                    false
                }
            } else {
                logger.error(
                    "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                    realmName,
                    "keycloak org admin",
                    response.status,
                    response.readEntity(String::class.java)
                )
                false
            }
        } catch (e: WebApplicationException) {
            logger.error(
                "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                realmName,
                "keycloak org admin",
                e.response.status,
                e.response.readEntity(String::class.java)
            )
            return false
        }
    }

    fun deleteKeycloakUser(realmName: String, adminUserName: String) {
        try {
            keycloakMaster.realm(realmName).users().search(adminUserName).firstOrNull()?.id?.let {
                keycloakMaster.realm(realmName).users().delete(it)
            }
        } catch (e: WebApplicationException) {
            logger.error(
                "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                realmName,
                "keycloak rollback org admin",
                e.response.status,
                e.response.readEntity(String::class.java)
            )
        }
    }

    fun createAdminUser(request: CreateAdminUserRequest): ResponseEntity<CreateAdminUser201Response> {
        val messengerInstanceEntity = messengerInstanceRepository.findByServerName(request.instanceName)

        val adminUser: GetMessengerInstanceInitialAdminCreds200Response = getOrgAdminCredentials(request.instanceName)
        val statusCode: HttpStatus = messengerInstanceEntity?.let { messengerInstance ->
            orgAdminManagementService.getByServerName(messengerInstance.serverName)?.let {
                HttpStatus.CONFLICT
            } ?: run {
                val keycloakAdminUserCreated = createKeycloakAdminUser(
                    realmName = messengerInstance.instanceId,
                    user = adminUser,
                    emailAddress = request.orgAdminEmailAddress
                )
                if (keycloakAdminUserCreated) {
                    try {
                        createOrgAdminEntity(messengerInstance, adminUser)
                        HttpStatus.CREATED
                    } catch (e: Exception) {
                        logger.error(ORG_ADMIN_ERROR_LOG_TEMPLATE, request.instanceName, "database", e)
                        HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }else {
                    deleteKeycloakUser(messengerInstance.instanceId, adminUser.username)
                    HttpStatus.INTERNAL_SERVER_ERROR
                }
            }
        } ?: HttpStatus.NOT_FOUND

        return if (statusCode.is2xxSuccessful) {
            ResponseEntity.status(statusCode).body(CreateAdminUser201Response(
                username = adminUser.username,
                password = adminUser.password,
            ))
        } else {
            ResponseEntity.status(statusCode).build()
        }
    }

    private fun getOrgAdminCredentials(serverName: String): GetMessengerInstanceInitialAdminCreds200Response =
        operatorService.getOrgAdminCredentials(serverName)

    fun getEnabledOrderUsersWithAttributes(): List<UserRepresentation> {
        val timRealm = keycloakTim.realm(keycloakProperties.timRealm.realmName)
        val users = timRealm.users().searchByAttributes("enabled=true")
        return users.filter {
            it.attributes.keys.containsAll(
                KeycloakUserAttributeKey.entries.map { attributeKey ->
                    attributeKey.value
                }
            )
        }
    }

    private fun createOrgAdminEntity(
        instanceEntity: MessengerInstanceEntity,
        adminUser: GetMessengerInstanceInitialAdminCreds200Response
    ): OrgAdminEntity = orgAdminManagementService.createOrgAdmin(
        serverName = instanceEntity.serverName,
        telematikId = instanceEntity.telematikId,
        mxId = "@${adminUser.username}:${instanceEntity.publicBaseUrl}",
        professionOid = instanceEntity.professionId
    )

    private fun preparePasswordRepresentation(
        password: String
    ): CredentialRepresentation {
        val credentialRepresentation = CredentialRepresentation()
        credentialRepresentation.isTemporary = false
        credentialRepresentation.type = CredentialRepresentation.PASSWORD
        credentialRepresentation.value = password
        return credentialRepresentation
    }

    private fun prepareUserRepresentation(
        userName: String,
        cR: CredentialRepresentation,
        emailAddress: String? = null
    ): UserRepresentation {
        val newUser = UserRepresentation()
        newUser.username = userName
        newUser.credentials = listOf(cR)
        newUser.isEnabled = true
        newUser.email = emailAddress
        return newUser
    }
}
