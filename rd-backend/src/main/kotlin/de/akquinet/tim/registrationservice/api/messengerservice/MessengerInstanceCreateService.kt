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

import de.akquinet.tim.registrationservice.api.federation.FederationService
import de.akquinet.tim.registrationservice.api.federation.model.Domain
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakRealmService
import de.akquinet.tim.registrationservice.api.messengerservice.model.InstanceCreateParams
import de.akquinet.tim.registrationservice.api.messengerservice.model.InstanceCreateResults
import de.akquinet.tim.registrationservice.api.operator.OperatorService
import de.akquinet.tim.registrationservice.config.KeycloakAdminConfig
import de.akquinet.tim.registrationservice.config.OperatorConfig
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.extension.toJson
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.rawdata.RawDataService
import de.akquinet.tim.registrationservice.rawdata.model.Operation
import de.akquinet.tim.registrationservice.util.ClaimNotFoundException
import de.akquinet.tim.registrationservice.util.NotLoggedInException
import de.akquinet.tim.registrationservice.util.UserService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Service
class MessengerInstanceCreateService @Autowired constructor(
    private val logger: Logger,
    private val operatorConfig: OperatorConfig,
    private val regServiceConfig: RegServiceConfig,
    private val operatorService: OperatorService,
    private val federationService: FederationService,
    private val rawdataService: RawDataService,
    private val messengerInstanceService: MessengerInstanceService,
    private val keycloakAdminConfig: KeycloakAdminConfig,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val keycloakRealmService: KeycloakRealmService,
    private val userService: UserService,
) {

    companion object {
        const val MAX_ROUNDS = 100
        const val INSTANCE_NAME_LENGTH: Int = 3
        val CHARACTER_POOL: List<Char> = ('a'..'z') + ('0'..'9')
        const val X_HEADER_INSTANCE_RANDOM = "x-inran"
        const val ERROR_LOG_TEMPLATE = "Error creating messenger instance ({}): {}"
    }

    private fun getCreateParams(): InstanceCreateParams {
        val dateOfOrder = LocalDate.parse(
            userService.loadUserAttributeByClaim("date_of_order"),
            DateTimeFormatter.ofPattern(
                "dd.MM.yyyy",
                Locale.GERMAN
            )
        )
        logger.debug("User Date of Order {}", dateOfOrder)

        val endDate = dateOfOrder.plusMonths(userService.loadUserAttributeByClaim("runtime").toLong())
        logger.debug("User endDate for instances {}", endDate)

        val instancesSize = messengerInstanceService.getAllInstancesForCurrentUser().size
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

    @ExperimentalTime
    fun createNewInstance(
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<String> {
        var serverName = ""
        val (responseEntity, elapsed) = measureTimedValue {
            try {
                val createParams = getCreateParams()
                val isNewInstanceAllowed = checkNewInstanceAllowed(createParams)

                if (isNewInstanceAllowed.first.is2xxSuccessful) {
                    val createResult = createInstanceWithParams(createParams)
                    if (createResult.isSuccessfull()) {
                        createResult.messengerInstanceEntity?.serverName?.let { serverName = it }
                        ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(X_HEADER_INSTANCE_RANDOM, createParams.instanceName)
                            .body("Erstelle Instanz mit Servername: $serverName und URL: $serverName".toJson())
                    } else {
                        logger.warn("Could not create messenger instance. Trying to rollback everything if necessary...")
                        val messages = performRollbackWhereNecessary(createParams, createResult).joinToString(", ")
                        logger.info("Rollback completed with message(s): {}", messages)
                        ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(messages.toJson())
                    }
                } else {
                    logger.warn("Could not create messenger instance: {}", isNewInstanceAllowed.second ?: "no details available")
                    ResponseEntity
                        .status(isNewInstanceAllowed.first)
                        .body(isNewInstanceAllowed.second?.toJson())
                }
            } catch (e: Exception) {
                when (e) {
                    is ClaimNotFoundException,
                    is NotLoggedInException ->
                        ResponseEntity
                            .status(HttpStatus.PRECONDITION_FAILED)
                            .body("One ore more required parameter(s) (date_of_order, runtime, telematik_id, profession_oid, user_id) not found.".toJson())
                    else -> {
                        logger.error(ERROR_LOG_TEMPLATE, serverName, "Unexpected", e)

                        ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Unexpected error, could not create new messenger instance".toJson())
                    }
                }
            }
        }

        performRawdataTasks(httpServletRequest, responseEntity, elapsed, serverName)

        return responseEntity
    }

    private fun performRollbackWhereNecessary(
        createParams: InstanceCreateParams,
        createResults: InstanceCreateResults
    ): List<String> {
        val result = mutableListOf<String>()
        if (createResults.messengerInstancePersisted) {
            createResults.messengerInstanceEntity?.let {
                messengerInstanceRepository.delete(it)
                result.add("Messenger instance: ${createParams.instanceFQDN} rolled back.")
            }
        } else {
            result.add("Messenger instance: ${createParams.instanceFQDN} was not persisted.")
        }

        if (createResults.keycloakRealmCreated) {
            keycloakRealmService.deleteRealmKeycloak(createParams.instanceId)
            result.add("Keycloak realm ${createParams.instanceId} rolled back.")
        } else {
            result.add("Keycloak realm was not created.")
        }

        if (createResults.federationListUpdated) {
            createResults.messengerInstanceEntity?.serverName?.let {
                federationService.deleteDomainFromVZDFederationList(it)
                result.add("VZD entry $it rolled back.")
            }
        } else {
            result.add("Federation list: was not updated with ${createParams.instanceFQDN}.")
        }

        if (createResults.operatorInstanceCreated) {
            createResults.messengerInstanceEntity?.let {
                operatorService.deleteInstanceOperator(it)
                result.add("Operator: instance ${createParams.instanceFQDN} (kubernetes namespace) rolled back.")
            }
        } else {
            result.add("Operator: instance ${createParams.instanceFQDN} was not created.")
        }

        return result
    }

    private fun createInstanceWithParams(createParams: InstanceCreateParams): InstanceCreateResults {
        val messengerInstance = createParams.toMessengerInstanceEntity()
        val clientSecret = keycloakRealmService.createRealmKeycloakAndGetSecret(messengerInstance)
        val keycloakRealmCreated = !clientSecret.isNullOrEmpty()
        if (!keycloakRealmCreated) {
            logger.warn("Could not create realm {} in keycloak.", messengerInstance.instanceId)
            return InstanceCreateResults()
        }
        val messengerInstancePersisted = persistMessengerInstance(subject = messengerInstance)
        if (!messengerInstancePersisted.first) {
            logger.warn("Could not persist messenger instance entity with servername = {} in registration service db", messengerInstance.serverName)
            return InstanceCreateResults(keycloakRealmCreated = true)
        }
        val federationListUpdated = updateVZDFederationList(subject = messengerInstance)
        if (!federationListUpdated) {
            logger.warn("Could not update federation list with entry for serverName {}", messengerInstance.serverName)
            return InstanceCreateResults(
                keycloakRealmCreated = true,
                messengerInstancePersisted = true
            )
        }
        val operatorInstanceCreated = clientSecret?.let {
            createOperatorInstance(subject = messengerInstance, clientSecret = it)
        } ?: false

        if (!operatorInstanceCreated) {
            logger.warn("Operator could not create instance with name {}", messengerInstance.serverName)
        }

        return InstanceCreateResults(
            keycloakRealmCreated = true,
            messengerInstancePersisted = true,
            messengerInstanceEntity = messengerInstancePersisted.second,
            federationListUpdated = true,
            operatorInstanceCreated = operatorInstanceCreated
        )
    }

    private fun persistMessengerInstance(
        subject: MessengerInstanceEntity
    ): Pair<Boolean, MessengerInstanceEntity?> = try {
        val persisted = messengerInstanceRepository.save(subject)
        Pair(true, persisted)
    } catch (e: Exception) {
        logger.error(ERROR_LOG_TEMPLATE, subject.serverName, "database", e)
        Pair(false, null)
    }

    private fun updateVZDFederationList(subject: MessengerInstanceEntity) =
        if (regServiceConfig.callExternalServices) {
            try {
                val vzdResponse = federationService.updateVZDFederationList(
                    Domain(subject.serverName, false, subject.telematikId)
                )

                if (vzdResponse.httpStatus != HttpStatus.OK) {
                    logger.warn("Unexpected status in VZD response: {}", vzdResponse.httpStatus)
                }

                vzdResponse.httpStatus == HttpStatus.OK
            } catch (e: Exception) {
                logger.error(ERROR_LOG_TEMPLATE, subject.serverName, "vzd", e)
                false
            }
        } else {
            // skip update of vzd federation list in local environment
            true
        }

    private fun createOperatorInstance(
        subject: MessengerInstanceEntity,
        clientSecret: String
    ) = if (regServiceConfig.callExternalServices) {
        operatorService.createInstanceOperator(
            messengerInstanceEntity = subject,
            clientSecret = clientSecret,
            issuer = keycloakAdminConfig.properties.masterRealm.url + "realms/${subject.instanceId}"
        ) == HttpStatus.CREATED
    } else {
        // skip creation of new instance in local environment
        true
    }

    private fun performRawdataTasks(
        request: HttpServletRequest,
        response: ResponseEntity<String>,
        duration: Duration,
        instanceName: String
    ) {
        rawdataService.collectAndSendRawData(
            request.getHeader("Content-Length")?.toIntOrNull() ?: 0,
            response.body?.length ?: 0,
            response.statusCode,
            duration,
            Operation.RS_CREATE_MESSENGER_SERVICE,
            instanceName
        )
    }


    fun randomInstanceName(): String = List(INSTANCE_NAME_LENGTH) { CHARACTER_POOL.random() }.joinToString("")

    fun generateAvailableInstanceName(baseFQDN: String): String {
        for (i in 0..<MAX_ROUNDS) {
            val result = randomInstanceName()
            val fqdn = result + baseFQDN
            if (messengerInstanceRepository.findAllByServerNameOrPublicBaseUrl(fqdn, fqdn).isEmpty()) {
                return result
            }
        }

        throw IllegalStateException("Could not find an available instance name (tries=$MAX_ROUNDS)")
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
