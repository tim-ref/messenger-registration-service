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

package de.akquinet.timref.registrationservice.api.messengerproxy

import de.akquinet.timref.registrationservice.config.MessengerProxyConfig
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.timref.registrationservice.util.UserService
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class MessengerProxyLogLevelService @Autowired constructor(
    private val logger: Logger,
    private val messengerProxyConfig: MessengerProxyConfig,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val userService: UserService,
    private val restTemplate: RestTemplate
){

    fun changeLogLevel(
        serverName: String,
        logLevel: String,
        loggerIdentifier: String = Logger.ROOT_LOGGER_NAME
    ): ResponseEntity<String> {
        val statusCode: HttpStatus
        val instanceEntity = messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
            serverName,
            userService.getUserIdFromContext()
        )
        statusCode = if (instanceEntity != null) {
            val proxyResponse = changeProxyInstanceLogLevel(
                instanceEntity.instanceId,
                logLevel.uppercase(),
                loggerIdentifier
            )
            if (proxyResponse.is2xxSuccessful) {
                return ResponseEntity.ok().build()
            } else {
                HttpStatus.INTERNAL_SERVER_ERROR
            }
        } else {
            HttpStatus.NOT_FOUND
        }

        val responseString = when (statusCode) {
            HttpStatus.INTERNAL_SERVER_ERROR -> {
                // description of 500 is used in frontend, please change it there as well if you are making changes here
                "\"Error on changing log level through operator\""
            }

            HttpStatus.NOT_FOUND -> {
                "\"Messenger Instance could not be found\""
            }

            else -> {
                "\"Interal Server Error in Backend during log level change\""

            }
        }
        return ResponseEntity.status(statusCode).body(responseString)
    }

    private fun changeProxyInstanceLogLevel(
        serverName: String,
        logLevel: String,
        loggerIdentifier: String
    ): HttpStatusCode {
        val requestSpecificPath = "$logLevel/$loggerIdentifier"
        val internalProxyInstanceUrl = buildInternalProxyInstanceUrl(serverName, requestSpecificPath)
        val proxyResponse = restTemplate.exchange(
            internalProxyInstanceUrl,
            HttpMethod.PUT,
            HttpEntity.EMPTY,
            String::class.java
        )

        if (proxyResponse.statusCode.is2xxSuccessful) {
            logger.info("Changed log level to {} for logger {} at proxy instance {}", logLevel, loggerIdentifier, serverName)
            return HttpStatus.OK
        }

        logger.error("Error changing instance logLevel through operator, status: ${proxyResponse.statusCode}")
        return HttpStatus.INTERNAL_SERVER_ERROR
    }

    fun buildInternalProxyInstanceUrl(serverName: String, specificPath: String): URI {
        val scheme = messengerProxyConfig.scheme
        val host = "${messengerProxyConfig.hostNamePrefix}.$serverName.${messengerProxyConfig.hostNameSuffix}"
        val port = messengerProxyConfig.actuatorPort
        val path = "${messengerProxyConfig.actuatorLoggingBasePath}/$specificPath"
        return URI.create("$scheme$host:$port$path")
    }
}