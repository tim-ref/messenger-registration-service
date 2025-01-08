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

package de.akquinet.tim.registrationservice.api.delegate

import de.akquinet.tim.registrationservice.api.keycloak.KeycloakOperationResult
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakRealmService
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserService
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceService
import de.akquinet.tim.registrationservice.api.messengerservice.OperatorInstanceCreateService
import de.akquinet.tim.registrationservice.api.messengerservice.OperatorInstanceDeleteService
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateAdminUserRequest
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateMessengerInstanceRequest
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateRealmRequest
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateRealmResponse
import de.akquinet.tim.registrationservice.openapi.model.mi.UpdateFederationListRequest
import de.akquinet.tim.registrationservice.openapi.api.operator.server.OperatorInstanceApiDelegate
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import java.util.Optional
import kotlin.time.ExperimentalTime

@Component
class OperatorInstanceApiDelegateImpl @Autowired constructor(
    private val request: NativeWebRequest,
    private val operatorInstanceCreateService: OperatorInstanceCreateService,
    private val operatorInstanceDeleteService: OperatorInstanceDeleteService,
    private val keycloakRealmService: KeycloakRealmService,
    private val keycloakUserService: KeycloakUserService,
    private val messengerInstanceService: MessengerInstanceService,
    private val messengerInstanceRepository: MessengerInstanceRepository
) : OperatorInstanceApiDelegate {

    @OptIn(ExperimentalTime::class)
    override fun createMessengerInstance(createMessengerInstanceRequest: CreateMessengerInstanceRequest): ResponseEntity<Unit> {
        val request: HttpServletRequest = getRequest()
            .map { it.nativeRequest as HttpServletRequest }
            .orElseThrow { RuntimeException("Could not extract HttpServletRequest") }

        return operatorInstanceCreateService.createNewInstance(request, createMessengerInstanceRequest)
    }

    override fun createRealm(createRealmRequest: CreateRealmRequest): ResponseEntity<CreateRealmResponse> {
        val result: Triple<CreateRealmResponse?, HttpStatus, String?> = messengerInstanceRepository.findByServerName(createRealmRequest.instanceName)?.let { entity ->
            entity.instanceId = createRealmRequest.realmName
            val saved = messengerInstanceRepository.save(entity)
            if (saved.instanceId.isNotEmpty()) {
                val keycloakResult = keycloakRealmService.createRealm(createRealmRequest)
                if (keycloakResult in setOf(KeycloakOperationResult.REALM_CREATED, KeycloakOperationResult.REALM_ALREADY_PRESENT)) {
                    keycloakRealmService.getSynapseClientSecretForRealm(createRealmRequest.realmName)?.let {
                        Triple(CreateRealmResponse(secret = it), HttpStatus.CREATED, null)
                    }
                } else {
                    Triple(null, HttpStatus.INTERNAL_SERVER_ERROR,"Could not create realm in keycloak with realmName ${createRealmRequest.realmName}")
                }
            }else {
                Triple(null, HttpStatus.INTERNAL_SERVER_ERROR,"Could not update messenger instance entity with realmName ${createRealmRequest.realmName}")
            }
        } ?: Triple(null, HttpStatus.PRECONDITION_FAILED,"Could not find messenger instance with name ${createRealmRequest.instanceName}")

        return result.first?.let {
            ResponseEntity.status(result.second).body(it)
        } ?: ResponseEntity
            .status(result.second)
            .header("x-error", result.third ?: "no details available")
            .build()

    }

    override fun updateFederationListAtVZD(
        updateFederationListRequest: UpdateFederationListRequest
    ): ResponseEntity<Unit> =
        if (operatorInstanceCreateService.updateVZDFederationList(updateFederationListRequest)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.internalServerError().build()
        }

    override fun createAdminUser(
        createAdminUserRequest: CreateAdminUserRequest
    ) = keycloakUserService.createAdminUser(createAdminUserRequest)

    override fun deleteMessengerService(instanceName: String): ResponseEntity<Unit> =
        operatorInstanceDeleteService.deleteInstance(instanceName)

    override fun getRequest() = Optional.of(request)
}