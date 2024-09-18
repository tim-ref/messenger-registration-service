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

package de.akquinet.tim.registrationservice.unitTests.service.messengerservice

import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceCreateService
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class MessengerInstanceCreateServiceTest: DescribeSpec() {

    init {
        describe("MessengerInstanceCreateServiceTest") {

            it("can generate random instance name") {
                val repo: MessengerInstanceRepository = mockk {
                    every { findAllByServerNameOrPublicBaseUrl(any(), any()) } returns emptyList()
                }
                val sut = MessengerInstanceCreateService(
                    messengerInstanceRepository = repo,
                    userService = mockk {},
                    rawdataService = mockk {},
                    federationService = mockk {},
                    logger = mockk {},
                    operatorService = mockk {},
                    regServiceConfig = mockk {},
                    operatorConfig = mockk {},
                    keycloakAdminConfig = mockk {},
                    keycloakRealmService = mockk {},
                    messengerInstanceService = mockk {}
                )

                val actual = sut.generateAvailableInstanceName("localhost")
                actual shouldHaveLength 3
                actual shouldMatch "^[a-z0-9]{3}$".toRegex()
            }

            it("can throw if MAX_ROUNDS is exceeded") {
                val repo: MessengerInstanceRepository = mockk {
                    every { findAllByServerNameOrPublicBaseUrl(any(), any()) } returns listOf(
                        MessengerInstanceEntity(
                            dateOfOrder = LocalDate.now(),
                            endDate = LocalDate.now().plusMonths(1),
                            userId = "someUser",
                            professionId = "someProfessionId",
                            publicBaseUrl = "some.server.name",
                            instanceId = "some.server.name".replace(".", ""),
                            serverName = "some.server.name",
                            telematikId = "someTelematikId"
                        )
                    )
                }
                val sut = MessengerInstanceCreateService(
                    messengerInstanceRepository = repo,
                    userService = mockk {},
                    rawdataService = mockk {},
                    federationService = mockk {},
                    logger = mockk {},
                    operatorService = mockk {},
                    regServiceConfig = mockk {},
                    operatorConfig = mockk {},
                    keycloakAdminConfig = mockk {},
                    keycloakRealmService = mockk {},
                    messengerInstanceService = mockk {}
                )

                val exception = shouldThrow<IllegalStateException> { sut.generateAvailableInstanceName("localhost")  }
                exception.message shouldBe "Could not find an available instance name (tries=${MessengerInstanceCreateService.MAX_ROUNDS})"
            }
        }
    }
}
