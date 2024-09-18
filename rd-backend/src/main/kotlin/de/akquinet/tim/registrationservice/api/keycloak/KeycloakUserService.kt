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

package de.akquinet.tim.registrationservice.api.keycloak

import com.google.gson.Gson
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceService.Companion.ORG_ADMIN_ERROR_LOG_TEMPLATE
import de.akquinet.tim.registrationservice.api.operator.AdminUser
import de.akquinet.tim.registrationservice.api.operator.OperatorService
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.persistance.orgAdmin.model.OrgAdminEntity
import de.akquinet.tim.registrationservice.service.orgadmin.OrgAdminManagementService
import de.akquinet.tim.registrationservice.util.UserService
import jakarta.ws.rs.WebApplicationException
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class KeycloakUserService @Autowired constructor(
    private val logger: Logger,
    private val regServiceConfig: RegServiceConfig,
    private val keycloak: Keycloak,
    private val userService: UserService,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val orgAdminManagementService: OrgAdminManagementService,
    private val operatorService: OperatorService,
) {

    private val gson = Gson()

    fun createKeycloakAdminUser(serverName: String, user: AdminUser): Boolean {
        val password = preparePasswordRepresentation(user.password)
        val userName = prepareUserRepresentation(user.userName, password)

        return try {
            val realmResource = keycloak.realm(serverName)
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
                    logger.error(ORG_ADMIN_ERROR_LOG_TEMPLATE, serverName, "keycloak admin roles", e)
                    deleteKeycloakUser(serverName, user)

                    false
                }
            } else {
                logger.error(
                    "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                    serverName,
                    "keycloak org admin",
                    response.status,
                    response.readEntity(String::class.java)
                )
                false
            }
        } catch (e: WebApplicationException) {
            logger.error(
                "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                serverName,
                "keycloak org admin",
                e.response.status,
                e.response.readEntity(String::class.java)
            )
            return false
        }
    }

    fun deleteKeycloakUser(serverName: String, adminUser: AdminUser) {
        try {
            val userId = keycloak.realm(serverName).users().search(adminUser.userName)[0].id
            keycloak.realm(serverName).users().delete(userId)
        } catch (e: WebApplicationException) {
            logger.error(
                "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                serverName,
                "keycloak rollback org admin",
                e.response.status,
                e.response.readEntity(String::class.java)
            )
        }
    }

    fun createAdminUser(serverName: String): ResponseEntity<String> {
        val messengerInstanceEntity = messengerInstanceRepository
            .findDistinctFirstByServerNameAndUserId(serverName, userService.getUserIdFromContext())

        val statusCode: HttpStatus = messengerInstanceEntity?.let { messengerInstance ->
            orgAdminManagementService.getByServerName(messengerInstance.serverName)?.let {
                HttpStatus.CONFLICT
            } ?: run {
                val adminUser = prepareAdminUser()
                try {
                    createOrgAdmin(messengerInstance, adminUser, serverName)
                } catch (e: Exception) {
                    logger.error(ORG_ADMIN_ERROR_LOG_TEMPLATE, serverName, "database", e)
                    HttpStatus.INTERNAL_SERVER_ERROR
                }

                val keycloakAdminUserCreated = createKeycloakAdminUser(messengerInstance.instanceId, adminUser)
                if (keycloakAdminUserCreated) {
                    val operatorResponse = createAdminOperator(serverName, adminUser)
                    if (operatorResponse.is2xxSuccessful) {
                        return ResponseEntity.status(HttpStatus.CREATED).body(gson.toJson(adminUser))
                    } else {
                        deleteKeycloakUser(serverName, adminUser)
                        HttpStatus.INTERNAL_SERVER_ERROR
                    }
                } else {
                    HttpStatus.INTERNAL_SERVER_ERROR
                }
            }
        } ?: HttpStatus.NOT_FOUND

        return ResponseEntity.status(statusCode).body(getResponseString(statusCode))
    }

    private fun createAdminOperator(serverName: String, adminUser: AdminUser) =
        if (regServiceConfig.callExternalServices) {
            operatorService.createOrgAdmin(serverName, adminUser)
        } else {
            HttpStatus.CREATED
        }

    private fun createOrgAdmin(
        instanceEntity: MessengerInstanceEntity,
        adminUser: AdminUser,
        serverName: String
    ): OrgAdminEntity = orgAdminManagementService.createOrgAdmin(
        serverName = instanceEntity.serverName,
        telematikId = instanceEntity.telematikId,
        mxId = "@${adminUser.userName}:$serverName",
        professionOid = instanceEntity.professionId
    )


    private fun getResponseString(statusCode: HttpStatus): String {
        val responseString = when (statusCode) {
            HttpStatus.CONFLICT -> "Admin user for for Instance already Exists"

            // description of 500 is used in frontend, please change it there as well if you are making changes here
            HttpStatus.INTERNAL_SERVER_ERROR -> "Error on creating admin user through operator"

            else -> "Messenger Instance could not be found"
        }
        return responseString
    }


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
        userName: String, cR: CredentialRepresentation
    ): UserRepresentation {
        val newUser = UserRepresentation()
        newUser.username = userName
        newUser.credentials = listOf(cR)
        newUser.isEnabled = true
        newUser.email = userService.loadUserAttributeByClaim("email")
        return newUser
    }

    private fun prepareAdminUser(): AdminUser {
        val userCharPool: List<Char> = ('a'..'z') + ('0'..'9')
        val passwordCharPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return AdminUser(
            userName = List(8) { userCharPool.random() }.joinToString(""),
            password = List(16) { passwordCharPool.random() }.joinToString("")
        )
    }
}
