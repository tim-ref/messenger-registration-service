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

package de.akquinet.tim.registrationservice.api.messengerservice

import de.akquinet.tim.registrationservice.api.federation.FederationListService
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakOperationResult
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakRealmService
import de.akquinet.tim.registrationservice.api.operator.OperatorService
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.extension.toJson
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.service.orgadmin.OrgAdminManagementService
import de.akquinet.tim.registrationservice.util.UserService
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service


@Service
class MessengerInstanceDeleteService @Autowired constructor(
    private val logger: Logger,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val userService: UserService,
    private val regServiceConfig: RegServiceConfig,
    private val federationListService: FederationListService,
    private val orgAdminManagementService: OrgAdminManagementService,
    private val keycloakRealmService: KeycloakRealmService,
    private val operatorService: OperatorService
) {
    companion object {
        const val ERROR_LOG_TEMPLATE = "Error deleting messenger instance ({}): {}, "
    }

    fun deleteInstance(serverName: String, userId: String? = null): ResponseEntity<String> {
        val user = userId ?: userService.getUserIdFromContext()

        val instanceEntity: MessengerInstanceEntity? =
            messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(serverName, user)

        var responseData: Pair<HttpStatus, String?> = Pair(HttpStatus.NOT_FOUND, "Messenger Instance not found")
        if (instanceEntity != null) {
            val vzdSuccess = deleteDomainFromVZD(serverName)
            val orgAdminSuccess = deleteOrgAdmin(instanceEntity)
            val operatorSuccess = deleteInstanceOperator(instanceEntity)
            val entitySuccess = deleteInstanceEntity(instanceEntity)
            val keycloakSuccess = deleteKeycloakRealm(instanceEntity)
            val operationResults: Set<Boolean> = setOf(
                vzdSuccess, orgAdminSuccess, operatorSuccess, entitySuccess, keycloakSuccess
            )

            responseData = if (operationResults.size == 1 && operationResults.first()) {
                Pair(HttpStatus.NO_CONTENT, null)
            } else {
                Pair(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete instance $serverName")
            }
        }

        return ResponseEntity
            .status(responseData.first)
            .body(responseData.second?.toJson())
    }

    private fun deleteDomainFromVZD(serverName: String) = if (regServiceConfig.callExternalServices) {
        try {
            val vzdResponse = federationListService.deleteDomainFromFederationListAtVzd(serverName)

            if (vzdResponse.httpStatus != HttpStatus.NO_CONTENT) {
                logger.warn("Unexpected status in vzd response: {}", vzdResponse.httpStatus)
            }

            vzdResponse.httpStatus == HttpStatus.NO_CONTENT
        } catch (e: Exception) {
            logger.error(ERROR_LOG_TEMPLATE, serverName, "vzd", e)
            false
        }
    } else {
        true
    }

    private fun deleteOrgAdmin(instanceEntity: MessengerInstanceEntity) =
        try {
            orgAdminManagementService.getByServerName(instanceEntity.serverName)?.let {
                orgAdminManagementService.delete(it)
            }

            true
        } catch (e: Exception) {
            logger.error(ERROR_LOG_TEMPLATE, instanceEntity.serverName, "org admin", e)
            false
        }

    private fun deleteInstanceOperator(instanceEntity: MessengerInstanceEntity) =
        if (regServiceConfig.callExternalServices) {
            val operatorResponseStatus = operatorService.deleteInstanceOperator(instanceEntity)

            if (operatorResponseStatus != HttpStatus.OK) {
                logger.warn("Unexpected status in operator response: {}", operatorResponseStatus)
            }

            operatorResponseStatus == HttpStatus.OK
        } else {
            // skip deletion of new instance in local and test environment
            true
        }

    private fun deleteInstanceEntity(instanceEntity: MessengerInstanceEntity) =
        try {
            messengerInstanceRepository.delete(instanceEntity)
            true
        } catch (e: Exception) {
            logger.error(ERROR_LOG_TEMPLATE, instanceEntity.serverName, "database", e)
            false
        }

    private fun deleteKeycloakRealm(instanceEntity: MessengerInstanceEntity) =
        try {
            val keycloakResult = keycloakRealmService.deleteRealmKeycloak(instanceEntity.instanceId)

            if (keycloakResult != KeycloakOperationResult.REALM_DELETED) {
                logger.warn("Unexpected keycloak result on realm delete: {}", keycloakResult)
            }

            keycloakResult == KeycloakOperationResult.REALM_DELETED
        } catch (e: Exception) {
            logger.error(ERROR_LOG_TEMPLATE, instanceEntity.serverName, "keycloak realm", e)
            false
        }

}
