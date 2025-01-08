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

package de.akquinet.tim.registrationservice.api.messengerservice

import de.akquinet.tim.registrationservice.api.messengerservice.model.InstanceCreateParams
import de.akquinet.tim.registrationservice.api.operator.OperatorService
import de.akquinet.tim.registrationservice.config.OperatorConfig
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.openapi.api.operator.client.SynapseOperatorApi
import de.akquinet.tim.registrationservice.openapi.model.mi.MessengerInstanceDto
import de.akquinet.tim.registrationservice.openapi.model.operator.CreateRequest
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfigurationProxy
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.util.UserService
import org.openapitools.client.infrastructure.ClientError
import org.openapitools.client.infrastructure.ResponseType
import org.openapitools.client.infrastructure.ServerError
import org.openapitools.client.infrastructure.Success
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private const val NO_DETAILS_AVAILABLE = "no details available"

@EnableScheduling
@Service
class MessengerInstanceService @Autowired constructor(
    private val logger: Logger,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val userService: UserService,
    private val regServiceConfig: RegServiceConfig,
    private val operatorService: OperatorService,
    private val operatorConfig: OperatorConfig,
    private val synapseOperatorApi: SynapseOperatorApi
) {

    companion object {
        const val ORG_ADMIN_ERROR_LOG_TEMPLATE = "Error creating org admin ({}): {}"
        const val ERROR_LOG_TEMPLATE = "Error creating messenger instance ({}): {}"
    }

    fun getAllInstancesForCurrentUser(userId: String? = null): List<MessengerInstanceDto> =
        messengerInstanceRepository.findAllByUserId(userId ?: userService.getUserIdFromContext())
            .map { it.toMessengerInstance() }

    fun getInstanceState(instanceId: String): InstanceStateDto {
        val instanceEntity = messengerInstanceRepository.findDistinctFirstByInstanceIdAndUserId(
            instanceId, userService.getUserIdFromContext()
        )

        val httpStatus = instanceEntity?.let {
            if (regServiceConfig.callExternalServices) {
                operatorService.operatorInstanceCheck(instanceEntity.serverName)
            } else {
                HttpStatus.OK
            }
        } ?: HttpStatus.NOT_FOUND

        return when (httpStatus) {
            HttpStatus.OK -> InstanceStateDto(isReady = true)

            // description of 500 is used in frontend, please change it there as well if you are making changes here
            HttpStatus.INTERNAL_SERVER_ERROR -> InstanceStateDto(
                isReady = false, message = "Error during operator instance check"
            )

            HttpStatus.NOT_FOUND -> InstanceStateDto(
                isReady = false, message = "Messenger Instance could not be found"
            )

            else -> InstanceStateDto(
                isReady = false, message = "Interal Server Error in Backend during instance ready check"
            )
        }
    }

    fun setAuthConceptConfig(
        instanceName: String, authConceptConfig: SynapseOverrideConfigurationProxy
    ): ResponseEntity<String> {
        val httpStatus = operatorService.setAuthConceptConfig(instanceName, authConceptConfig)
        return ResponseEntity.status(httpStatus).build()
    }

    fun getAuthConceptConfig(instanceName: String): ResponseEntity<SynapseOverrideConfigurationProxy> {
        val response = operatorService.getAuthConceptConfig(instanceName)

        return when (response.statusCode) {
            HttpStatus.OK -> ResponseEntity.ok().body(response.body)
            else -> ResponseEntity.status(response.statusCode).build()
        }
    }

    private fun getCreateParams(): InstanceCreateParams {
        val dateOfOrder = LocalDate.parse(
            userService.loadUserAttributeByClaim("date_of_order"), DateTimeFormatter.ofPattern(
                "dd.MM.yyyy", Locale.GERMAN
            )
        )
        logger.debug("User Date of Order {}", dateOfOrder)

        val endDate = dateOfOrder.plusMonths(userService.loadUserAttributeByClaim("runtime").toLong())
        logger.debug("User endDate for instances {}", endDate)

        val instancesSize = getAllInstancesForCurrentUser().size
        logger.debug("Number of instances for User {}", instancesSize)

        val professionOid = userService.loadUserAttributeByClaim("profession_oid")

        val telematikId = userService.loadUserAttributeByClaim("telematik_id")

        val instanceName = UUID.randomUUID().toString()
        val instanceFQDN = instanceName + operatorConfig.properties.baseFQDN
        logger.debug("FQDN: {}", instanceFQDN)

        return InstanceCreateParams(
            userId = userService.getUserIdFromContext(),
            dateOfOrder = dateOfOrder,
            endDate = endDate,
            currentInstanceCount = instancesSize,
            telematikId = telematikId,
            instanceName = instanceName,
            instanceFQDN = instanceFQDN,
            professionOid = professionOid,
            active = true,
            startOfInactivity = null
        )
    }

    fun requestNewInstance(): ResponseEntity<Unit> {
        val allowedCheckResponse = checkNewInstanceAllowed(getCreateParams())
        return if (allowedCheckResponse.first.is2xxSuccessful) {
            val response = if (regServiceConfig.callExternalServices) {
                synapseOperatorApi.createMessengerInstanceWithHttpInfo(
                    CreateRequest(
                        instanceOwner = userService.getUserIdFromContext(),
                        telematikId = userService.loadUserAttributeByClaim("telematik_id"),
                        professionOid = userService.loadUserAttributeByClaim("profession_oid")
                    )
                )
            } else {
                Success(statusCode = 202, data = null)
            }

            when (response.responseType) {
                ResponseType.Success -> logger.debug(
                    "Operator: Create messenger request was successful ({}).", response.statusCode
                )

                ResponseType.ClientError -> logger.error(
                    "Operator: Could not create messenger instance: {}",
                    (response as ClientError).message ?: NO_DETAILS_AVAILABLE
                )

                ResponseType.ServerError -> logger.error(
                    "Operator: Could not create messenger instance: {}",
                    (response as ServerError).message ?: NO_DETAILS_AVAILABLE
                )

                else -> logger.warn("Operator: Unexpected result for create messenger request: {}", response.statusCode)
            }
            ResponseEntity.status(response.statusCode).build()
        } else {
            ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build()
        }
    }

    private fun checkNewInstanceAllowed(params: InstanceCreateParams): Pair<HttpStatus, String?> {
        val result = if (LocalDate.now().isAfter(params.endDate)) {
            Pair(
                HttpStatus.PRECONDITION_FAILED,
                "Die maximale Laufzeit Ihrer Messenger-Services ist bereits erreicht: Sie können keine weiteren Instanzen bestellen"
            )
        } else if (params.currentInstanceCount == userService.loadUserAttributeByClaim("instances").toInt()) {
            Pair(
                HttpStatus.FORBIDDEN,
                "Die maximale Anzahl Messenger-Service Instanzen ist bereits erreicht: Sie können keine weiteren Instanzen bestellen"
            )
        } else {
            Pair(HttpStatus.OK, null)
        }

        logger.debug("checkNewInstanceAllowed: {}", result.first)
        return result
    }
}

data class InstanceStateDto(
    val isReady: Boolean,
    val message: String? = null,
)
