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

package de.akquinet.tim.registrationservice.api.operator

import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceDeleteService
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceService.Companion.ORG_ADMIN_ERROR_LOG_TEMPLATE
import de.akquinet.tim.registrationservice.config.OperatorConfig
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.openapi.model.operator.GetMessengerInstanceInitialAdminCreds200Response
import de.akquinet.tim.registrationservice.openapi.api.operator.client.SynapseOperatorApi
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfiguration
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfigurationProxy
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import org.openapitools.client.infrastructure.Success
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class OperatorService @Autowired constructor(
    private val logger: Logger,
    private val operatorConfig: OperatorConfig,
    private val regServiceConfig: RegServiceConfig,
    private val operatorApi: SynapseOperatorApi,
) {
    fun setAuthConceptConfig(
        instanceName: String, authConceptConfig: SynapseOverrideConfigurationProxy
    ): HttpStatus = if (regServiceConfig.callExternalServices) {
        try {
            val operatorResponse = operatorApi.changeMessengerInstanceConfigurationWithHttpInfo(
                instanceName, SynapseOverrideConfiguration(proxy = authConceptConfig)
            )
            if (operatorResponse !is Success) {
                logger.error(
                    "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                    instanceName,
                    "error while setting timAuthConceptConfig",
                    operatorResponse.statusCode,
                    null
                )
                HttpStatus.INTERNAL_SERVER_ERROR
            } else {
                HttpStatus.OK
            }
        } catch (e: Exception) {
            HttpStatus.INTERNAL_SERVER_ERROR
        }
    } else {
        HttpStatus.OK
    }

    fun getAuthConceptConfig(
        instanceName: String,
    ): ResponseEntity<SynapseOverrideConfigurationProxy> = if (regServiceConfig.callExternalServices) {
        try {
            val operatorResponse = operatorApi.getMessengerInstanceConfigurationWithHttpInfo(instanceName)
            if (operatorResponse !is Success) {
                logger.error(
                    "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                    instanceName,
                    "error while getting timAuthConceptConfig",
                    operatorResponse.statusCode,
                    null
                )
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            } else {
                ResponseEntity.status(HttpStatus.OK).body(operatorResponse.data?.proxy)
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()

        }
    } else {
        ResponseEntity.ok().body(
            SynapseOverrideConfigurationProxy(
                SynapseOverrideConfigurationProxy.FederationCheckConcept.PROXY,
                SynapseOverrideConfigurationProxy.InviteRejectionPolicy.ALLOW_ALL
            )
        )
    }


    fun deleteInstanceOperator(messengerInstanceEntity: MessengerInstanceEntity): HttpStatus = try {
        val operatorResponse = operatorApi.deleteMessengerInstanceWithHttpInfo(messengerInstanceEntity.instanceId)

        if (operatorResponse.statusCode != HttpStatus.ACCEPTED.value()) {
            logger.error(
                "${MessengerInstanceDeleteService.ERROR_LOG_TEMPLATE}, status: {}, message: {}",
                messengerInstanceEntity.serverName,
                "operator",
                operatorResponse.statusCode,
                null
            )
            HttpStatus.INTERNAL_SERVER_ERROR
        }
        HttpStatus.OK
    } catch (e: Exception) {
        logger.error(
            MessengerInstanceDeleteService.ERROR_LOG_TEMPLATE, messengerInstanceEntity.serverName, "operator", e
        )

        HttpStatus.INTERNAL_SERVER_ERROR
    }

    fun operatorInstanceCheck(instanceId: String): HttpStatus = if (regServiceConfig.callExternalServices) {
        val operatorResponse = operatorApi.getMessengerInstanceReadinessWithHttpInfo(instanceId.replace(".", ""))
        if (operatorResponse.statusCode != HttpStatus.OK.value()) {
            logger.error(
                "$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                instanceId,
                "read check",
                operatorResponse.statusCode,
                null
            )
            HttpStatus.INTERNAL_SERVER_ERROR
        } else {
            HttpStatus.OK
        }
    } else {
        HttpStatus.OK
    }


    fun getOrgAdminCredentials(serverName: String): GetMessengerInstanceInitialAdminCreds200Response =
        if (regServiceConfig.callExternalServices && operatorInstanceCheck(serverName).is2xxSuccessful) {
            operatorApi.getMessengerInstanceInitialAdminCreds(serverName.replace(".", ""))
        } else {
            GetMessengerInstanceInitialAdminCreds200Response(
                username = "test",
                password = "Test123!", // !! Must meet KeycloakRealmService.createRealm() { ... realmPasswordPolicy ... } !!
                userid = "test"
            )
        }
}
