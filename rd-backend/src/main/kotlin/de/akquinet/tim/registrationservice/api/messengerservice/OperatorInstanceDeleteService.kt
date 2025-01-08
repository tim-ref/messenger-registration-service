/*
 * Copyright (C) 2024 akquinet GmbH
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
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.service.orgadmin.OrgAdminManagementService
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class OperatorInstanceDeleteService @Autowired constructor(
    private val logger: Logger,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val regServiceConfig: RegServiceConfig,
    private val federationListService: FederationListService,
    private val orgAdminManagementService: OrgAdminManagementService,
    private val keycloakRealmService: KeycloakRealmService
) {

    companion object {
        const val ERROR_LOG_TEMPLATE = "Error deleting messenger instance ({}): {}, "
    }

    fun deleteInstance(instanceName: String): ResponseEntity<Unit> {
        val instanceEntity: MessengerInstanceEntity? = messengerInstanceRepository.findByServerName(instanceName)

        // delete keycloak realm if any
        val realmName = instanceEntity?.instanceId?.takeIf { it.isNotEmpty() }
        val keycloakSuccess = realmName?.let { deleteKeycloakRealm(it) } ?: true

        // delete orgAdmin if any
        val orgAdminSuccess = deleteOrgAdmin(instanceName)

        // update VZD by removing the instance from it if any
        val vzdSuccess = instanceEntity?.publicBaseUrl?.let { deleteDomainFromVZD(it) } ?: true

        // delete entity if any
        val entitySuccess = instanceEntity?.let { deleteInstanceEntity(it) } ?: true

        val operationResults: Set<Boolean> = setOf(
            keycloakSuccess, orgAdminSuccess, vzdSuccess, entitySuccess
        )

        val responseStatus = if (operationResults.size == 1 && operationResults.first()) {
            HttpStatus.NO_CONTENT
        } else {
            HttpStatus.INTERNAL_SERVER_ERROR
        }

        return ResponseEntity
            .status(responseStatus)
            .build()
    }

    fun deleteDomainFromVZD(domain: String) =
        if (regServiceConfig.callExternalServices) {
            try {
                val vzdResponse = federationListService.deleteDomainFromFederationListAtVzd(domain)
                when (vzdResponse.httpStatus) {
                    HttpStatus.NO_CONTENT -> true
                    HttpStatus.NOT_FOUND -> {
                        logger.warn("Could not find {} in VZD, VZD responded with 404 - not found", domain)
                        true
                    }
                    else -> {
                        logger.warn("Could not delete {} in VZD, VZD responded with {}", domain, vzdResponse.httpStatus)
                        false
                    }
                }
            } catch (e: Exception) {
                logger.error(ERROR_LOG_TEMPLATE, domain, "vzd", e)
                false
            }
        } else {
            true
        }

    private fun deleteOrgAdmin(instanceName: String) =
        try {
            orgAdminManagementService.getByServerName(instanceName)?.let {
                orgAdminManagementService.delete(it)
            }
            true
        } catch (e: Exception) {
            logger.error(ERROR_LOG_TEMPLATE, instanceName, "org admin", e)
            false
        }

    private fun deleteInstanceEntity(instanceEntity: MessengerInstanceEntity) =
        try {
            messengerInstanceRepository.delete(instanceEntity)
            true
        } catch (e: Exception) {
            logger.error(ERROR_LOG_TEMPLATE, instanceEntity.serverName, "database", e)
            false
        }

    private fun deleteKeycloakRealm(realmName: String) =
        try {
            keycloakRealmService.deleteRealmKeycloak(realmName) in
                    setOf(KeycloakOperationResult.REALM_DELETED, KeycloakOperationResult.REALM_NOT_DELETED)
        } catch (e: Exception) {
            logger.error(ERROR_LOG_TEMPLATE, realmName, "keycloak realm", e)
            false
        }

}