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

package de.akquinet.tim.registrationservice.api.messengerproxy

import de.akquinet.tim.registrationservice.openapi.api.operator.client.SynapseOperatorApi
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.util.UserService
import org.openapitools.client.infrastructure.Success
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MessengerProxyLogLevelService @Autowired constructor(
    private val logger: Logger,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val userService: UserService,
    private val synapseOperatorApi: SynapseOperatorApi
) {

    fun changeLogLevel(
        serverName: String,
        logLevel: String,
        loggerIdentifier: String = Logger.ROOT_LOGGER_NAME
    ): ResponseEntity<String> {
        val statusCode: HttpStatus =
            messengerInstanceRepository.findDistinctFirstByInstanceIdAndUserId(
                serverName,
                userService.getUserIdFromContext()
            )?.let {
                when (val response =
                    synapseOperatorApi.changeMessengerInstanceLoglevelWithHttpInfo(serverName, logLevel)
                ) {
                    is Success -> {
                        if (response.statusCode == HttpStatus.OK.value()) {
                            logger.info(
                                "Changed log level to {} for logger {} at proxy instance {}",
                                logLevel,
                                loggerIdentifier,
                                serverName
                            )
                            HttpStatus.OK
                        }else {
                            HttpStatus.INTERNAL_SERVER_ERROR
                        }
                    }
                    else -> {
                        logger.error("Error changing instance logLevel through operator, status: ${response.statusCode}")
                        HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            } ?: HttpStatus.NOT_FOUND

        val responseString = when (statusCode) {
            // description of 500 is used in frontend, please change it there as well if you are making changes here
            HttpStatus.INTERNAL_SERVER_ERROR -> "\"Error on changing log level through operator\""
            HttpStatus.NOT_FOUND -> "\"Messenger Instance could not be found\""
            else -> "\"Interal Server Error in Backend during log level change\""
        }
        return ResponseEntity.status(statusCode).body(responseString)
    }
}