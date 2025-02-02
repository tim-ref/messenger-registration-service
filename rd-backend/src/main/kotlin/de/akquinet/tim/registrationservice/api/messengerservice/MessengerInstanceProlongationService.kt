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

import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserAttributeKey
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserService
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MessengerInstanceProlongationService @Autowired constructor(
    private val keycloakUserService: KeycloakUserService,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val logger: Logger
) {

    @Scheduled(cron = "\${backend.periodOfValidityCheck.cron}", zone = "Europe/Berlin")
    fun alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase() {
        try {
            val orderUsers = keycloakUserService.getEnabledMessengerInstanceOrderUsers().filter {
                it.attributes.containsKey(KeycloakUserAttributeKey.TELEMATIK_ID.value) && it.attributes.containsKey(
                    KeycloakUserAttributeKey.PROFESSION_OID.value
                ) && it.attributes.containsKey(
                    KeycloakUserAttributeKey.ORDER_LENGTH.value
                )
            }.map {
                OrderUser(
                    username = it.username.trim(),
                    telematikId = it.firstAttribute(KeycloakUserAttributeKey.TELEMATIK_ID.value).trim(),
                    professionOid = it.firstAttribute(KeycloakUserAttributeKey.PROFESSION_OID.value).trim(),
                    orderLength = it.firstAttribute(KeycloakUserAttributeKey.ORDER_LENGTH.value).trim()
                )
            }

            logger.debug(
                "Retrieved {} order users from user service",
                orderUsers.size
            )

            orderUsers.forEach { user ->
                messengerInstanceRepository.findByUserIdAndTelematikIdAndProfessionId(
                    user.username, user.telematikId, user.professionOid
                ).let { instances ->
                    instances.forEach {
                        val newEndDate = it.dateOfOrder.plusMonths(user.orderLength.toLong())
                        if (!newEndDate.isEqual(it.endDate)) {
                            it.endDate = newEndDate
                            val saved = messengerInstanceRepository.save(it)
                            logger.info(
                                "Prolonged lifetime of instance with serverName={} for user {} with telematikId={} and professionOid={} until {}",
                                it.serverName, user.username, user.telematikId, user.professionOid, saved.endDate
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error aligning order length. The reason was: {}", e.message, e)
        }
    }
}

private data class OrderUser(
    val username: String,
    val telematikId: String,
    val professionOid: String,
    val orderLength: String
)
