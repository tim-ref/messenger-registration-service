/*
 * Copyright (C) 2024 - 2025 akquinet GmbH
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

import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserService
import de.akquinet.tim.registrationservice.api.messengerservice.model.toOrderUser
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MessengerInstanceProlongationService @Autowired constructor(
    private val keycloakUserService: KeycloakUserService,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val logger: Logger,
) {

    @Scheduled(cron = "\${backend.periodOfValidityCheck.cron}", zone = "Europe/Berlin")
    fun alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase() {
        try {
            val orderUsers = keycloakUserService.getEnabledOrderUsersWithAttributes().map {
                it.toOrderUser()
            }

            orderUsers.forEach { user ->
                val instances = messengerInstanceRepository.findByUserIdAndTelematikIdAndProfessionId(
                    userId = user.username, telematikId = user.telematikId, professionOid = user.professionOid
                )

                instances.forEach {
                    var shouldUpdateEntry = false
                    val newDateOfOrder = user.dateOfOrder

                    if (!it.dateOfOrder.isEqual(newDateOfOrder)) {
                        it.dateOfOrder = newDateOfOrder
                        shouldUpdateEntry = true
                    }

                    val newEndDate = it.dateOfOrder.plusMonths(user.orderLength.toLong())

                    if (!newEndDate.isEqual(it.endDate)) {
                        it.endDate = newEndDate
                        shouldUpdateEntry = true
                    }

                    if (shouldUpdateEntry) {
                        val saved = messengerInstanceRepository.save(it)
                        logger.info(
                            "Prolonged lifetime of {} for {} since {} until {}",
                            saved.serverName,
                            user.username,
                            saved.dateOfOrder,
                            saved.endDate
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error aligning order length. The reason was: {}", e.message, e)
        }
    }
}
