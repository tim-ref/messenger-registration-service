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

package de.akquinet.tim.registrationservice.api.delegate

import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserService
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceDeleteService
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceService
import de.akquinet.tim.registrationservice.openapi.api.mi.server.MessengerInstanceApiDelegate
import de.akquinet.tim.registrationservice.openapi.model.mi.*
import de.akquinet.tim.registrationservice.api.messengerservice.model.getOverrideConfigurationProxyFromResponseModel
import de.akquinet.tim.registrationservice.api.messengerservice.model.getOverrideConfigurationResponseFromModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import java.util.Optional

@Component
class MessengerInstanceApiDelegateImpl @Autowired constructor(
    private val request: NativeWebRequest,
    private val messengerInstanceService: MessengerInstanceService,
    private val messengerInstanceDeleteService: MessengerInstanceDeleteService,
    private val keycloakUserService: KeycloakUserService,
) : MessengerInstanceApiDelegate {

    override fun createAdminUser(createAdminUserRequest: CreateAdminUserRequest): ResponseEntity<CreateAdminUser201Response> =
        if (messengerInstanceService.getInstanceState(createAdminUserRequest.instanceName).isReady) {
            val response = keycloakUserService.createAdminUser(createAdminUserRequest)
            ResponseEntity.status(response.statusCode).body(response.body)
        } else {
            ResponseEntity.status(HttpStatus.LOCKED).build()
        }

    override fun putMessengerInstanceTimAuthConceptConfig(
        instanceName: String,
        getMessengerInstanceTimAuthConceptConfig200Response: GetMessengerInstanceTimAuthConceptConfig200Response
    ): ResponseEntity<Unit> {
        val response = messengerInstanceService.setAuthConceptConfig(
            instanceName,
            getOverrideConfigurationProxyFromResponseModel(getMessengerInstanceTimAuthConceptConfig200Response)
        )

        return ResponseEntity
            .status(response.statusCode)
            .headers(response.headers)
            .build()
    }


    override fun getMessengerInstanceTimAuthConceptConfig(instanceName: String): ResponseEntity<GetMessengerInstanceTimAuthConceptConfig200Response> {
        val response = messengerInstanceService.getAuthConceptConfig(instanceName)

        return ResponseEntity
            .status(response.statusCode)
            .headers(response.headers)
            .body(response.body?.let { getOverrideConfigurationResponseFromModel(it) })
    }


    override fun requestMessengerInstance(): ResponseEntity<Unit> {
        val response = messengerInstanceService.requestNewInstance()

        return ResponseEntity
            .status(response.statusCode)
            .headers(response.headers)
            .build()
    }

    override fun deleteMessengerService(instanceName: String): ResponseEntity<Unit> {
        val response = messengerInstanceDeleteService.deleteInstance(instanceName)
        return ResponseEntity.status(response.statusCode).build()
    }

    override fun getMessengerInstances(): ResponseEntity<List<MessengerInstanceDto>> =
        ResponseEntity.ok(messengerInstanceService.getAllInstancesForCurrentUser())

    override fun getRequest() = Optional.of(request)
}