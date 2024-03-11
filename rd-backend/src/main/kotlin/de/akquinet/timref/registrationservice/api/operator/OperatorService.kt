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

package de.akquinet.timref.registrationservice.api.operator

import com.google.gson.Gson
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceCreateService
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceDeleteService
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceService.Companion.ORG_ADMIN_ERROR_LOG_TEMPLATE
import de.akquinet.timref.registrationservice.config.OperatorConfig
import de.akquinet.timref.registrationservice.config.RegServiceConfig
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class OperatorService @Autowired constructor(
    private val logger: Logger,
    private val operatorConfig: OperatorConfig,
    private val regServiceConfig: RegServiceConfig,
    private val restTemplate: RestTemplate,
    @Qualifier("operator") private val restTemplateOperator: RestTemplate
) {

    private val gson = Gson()

    fun createInstanceOperator(
        messengerInstanceEntity: MessengerInstanceEntity, clientSecret: String, issuer: String
    ): HttpStatus =
        try {
            val config = gson.toJson(messengerInstanceEntity.toSynapseSpec(clientSecret, issuer))
            logger.debug("Config sent to Operator: {}", config)
            val request: HttpEntity<String> = HttpEntity<String>(config, basicHeaders())
            val operatorResponse: ResponseEntity<String> = restTemplate.postForEntity(
                operatorConfig.let { "${it.host}:${it.port}${it.createPath}" }, request, String::class.java
            )

            if (operatorResponse.statusCode != HttpStatus.CREATED) {
                logger.error(
                    "${MessengerInstanceCreateService.ERROR_LOG_TEMPLATE}, status: {}, message {}",
                    "operator",
                    operatorResponse.statusCode,
                    operatorResponse.body
                )
                HttpStatus.INTERNAL_SERVER_ERROR
            }
            HttpStatus.CREATED
        }catch (e: Exception) {
            HttpStatus.INTERNAL_SERVER_ERROR
        }

    fun deleteInstanceOperator(
        messengerInstanceEntity: MessengerInstanceEntity
    ): HttpStatus =
        try {
            val request: HttpEntity<*> = HttpEntity<Any>(basicHeaders())
            logger.info("Headers {}", request.headers.toString())
            logger.info("Body {}", request.body)
            val operatorResponse: ResponseEntity<String> = restTemplate.exchange(
                operatorConfig.let { "${it.host}:${it.port}${it.deletePath}/${messengerInstanceEntity.instanceId}" },
                HttpMethod.DELETE,
                request,
                String::class.java
            )

            if (operatorResponse.statusCode != HttpStatus.OK) {
                logger.error(
                    "${MessengerInstanceDeleteService.ERROR_LOG_TEMPLATE}, status: {}, message: {}",
                    messengerInstanceEntity.serverName,
                    "operator",
                    operatorResponse.statusCode,
                    operatorResponse.body)
                HttpStatus.INTERNAL_SERVER_ERROR
            }
            HttpStatus.OK
        }catch (e: Exception) {
            logger.error(
                MessengerInstanceDeleteService.ERROR_LOG_TEMPLATE,
                messengerInstanceEntity.serverName,
                "operator",
                e
            )
            HttpStatus.INTERNAL_SERVER_ERROR
        }

    fun operatorInstanceCheck(serverName: String): HttpStatus {
        if (regServiceConfig.callExternalServices) {
            val operatorResponse: ResponseEntity<String> = restTemplateOperator.getForEntity(
                operatorConfig.let { "${it.host}:${it.port}${it.createPath}/${serverName.replace(".", "")}/ready" },
                String::class.java
            )
            if (operatorResponse.statusCode != HttpStatus.OK) {
                logger.error("$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                    serverName,
                    "read check",
                    operatorResponse.statusCode,
                    operatorResponse.body
                )
                return HttpStatus.INTERNAL_SERVER_ERROR
            }
        }
        return HttpStatus.OK

    }

    fun createOrgAdmin(serverName: String, user: AdminUser): HttpStatus {
        if (regServiceConfig.callExternalServices) {

            val userJson = gson.toJson(user)
            val request: HttpEntity<String> = HttpEntity<String>(userJson, basicHeaders())
            val operatorResponse: ResponseEntity<String> = restTemplate.postForEntity(
                operatorConfig.let { "${it.host}:${it.port}${it.createPath}/${serverName.replace(".", "")}/admin" },
                request,
                String::class.java
            )
            if (operatorResponse.statusCode != HttpStatus.CREATED) {
                logger.error("$ORG_ADMIN_ERROR_LOG_TEMPLATE, status: {}, message: {}",
                    serverName,
                    "operator",
                    operatorResponse.statusCode,
                    operatorResponse.body)
                return HttpStatus.INTERNAL_SERVER_ERROR
            }
        }

        return HttpStatus.CREATED
    }

    private fun basicHeaders() = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        setBasicAuth(operatorConfig.username, operatorConfig.password)
    }
}
