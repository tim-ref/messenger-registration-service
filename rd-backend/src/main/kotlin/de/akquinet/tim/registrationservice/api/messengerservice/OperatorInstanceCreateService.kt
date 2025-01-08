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
import de.akquinet.tim.registrationservice.api.messengerservice.model.InstanceCreateParams
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.openapi.model.federation.Domain
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateMessengerInstanceRequest
import de.akquinet.tim.registrationservice.openapi.model.mi.UpdateFederationListRequest
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.rawdata.RawDataService
import de.akquinet.tim.registrationservice.rawdata.model.Operation
import de.akquinet.tim.registrationservice.util.UserService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Service
class OperatorInstanceCreateService @Autowired constructor(
    private val logger: Logger,
    private val regServiceConfig: RegServiceConfig,
    private val federationListService: FederationListService,
    private val rawdataService: RawDataService,
    private val messengerInstanceService: MessengerInstanceService,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val userService: UserService,
) {

    companion object {
        const val X_HEADER_INSTANCE_RANDOM = "x-inran"
        const val ERROR_LOG_TEMPLATE = "Error creating messenger instance ({}): {}"
    }

    fun getCreateParams(params: CreateMessengerInstanceRequest): InstanceCreateParams {
        val dateOfOrder = LocalDate.now()

        val instancesSize = messengerInstanceService.getAllInstancesForCurrentUser(params.userId).size
        val instanceId = UUID.randomUUID().toString()
        val result = InstanceCreateParams(
            userId = params.userId ?: userService.getUserIdFromContext(),
            dateOfOrder = dateOfOrder,
            endDate = dateOfOrder.plusYears(100),
            currentInstanceCount = instancesSize,
            telematikId = params.telematikId,
            instanceName = params.instanceName,
            instanceFQDN = params.publicHomeserverFQDN,
            professionOid = params.professionOid,
            active = true,
            startOfInactivity = null,
            instanceId = instanceId
        )

        logger.debug("Creating messenger instance with these parameters: {}", result)

        return result
    }

    @ExperimentalTime
    fun createNewInstance(
        request: HttpServletRequest,
        createRequestParams: CreateMessengerInstanceRequest
    ): ResponseEntity<Unit> {
        val (httpStatusCode, elapsed) = measureTimedValue {
            val createParams = getCreateParams(createRequestParams)
            persistMessengerInstance(createParams.toMessengerInstanceEntity())?.let {
                HttpStatus.CREATED
            } ?: HttpStatus.INTERNAL_SERVER_ERROR
        }

        performRawdataTasks(
            request,
            httpStatusCode,
            elapsed,
            createRequestParams.instanceName,
            createRequestParams.telematikId,
            createRequestParams.professionOid,
            createRequestParams.userId ?: userService.getUserIdFromContextSafely()
        )

        return if (httpStatusCode.isSameCodeAs(HttpStatus.CREATED)) {
            ResponseEntity
                .status(httpStatusCode)
                .header(X_HEADER_INSTANCE_RANDOM, createRequestParams.instanceName)
                .build()
        } else {
            ResponseEntity.status(httpStatusCode).build()
        }
    }

    private fun persistMessengerInstance(
        subject: MessengerInstanceEntity
    ): MessengerInstanceEntity? = try {
        messengerInstanceRepository.save(subject)
    } catch (e: Exception) {
        logger.error(ERROR_LOG_TEMPLATE, subject.serverName, "database", e)
        null
    }

    fun updateVZDFederationList(updateFederationListRequest: UpdateFederationListRequest) =
        if (regServiceConfig.callExternalServices) {
            messengerInstanceRepository.findByServerName(updateFederationListRequest.instanceName)?.let {
                try {
                    federationListService.addDomainToFederationListAtVzd(
                        Domain(
                            domain = updateFederationListRequest.publicHomeserverFQDN,
                            isInsurance = false,
                            telematikID = updateFederationListRequest.telematikId
                        ),
                        updateFederationListRequest
                    ).httpStatus == HttpStatus.OK
                } catch (e: Exception) {
                    logger.error(ERROR_LOG_TEMPLATE, updateFederationListRequest.publicHomeserverFQDN, "vzd", e)
                    false
                }
            } ?: false
        } else {
            true
        }

    private fun performRawdataTasks(
        request: HttpServletRequest,
        httpStatusCode: HttpStatusCode,
        duration: Duration,
        instanceName: String,
        telematikId: String,
        professionOid: String,
        userId: String
    ) {
        rawdataService.collectAndSendRawData(
            request.getHeader("Content-Length")?.toIntOrNull() ?: 0,
            0,
            httpStatusCode,
            duration,
            Operation.RS_CREATE_MESSENGER_SERVICE,
            instanceName,
            telematikId,
            professionOid,
            userId
        )
    }
}
