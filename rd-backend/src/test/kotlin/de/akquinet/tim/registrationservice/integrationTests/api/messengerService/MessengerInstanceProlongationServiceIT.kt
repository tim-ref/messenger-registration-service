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

package de.akquinet.tim.registrationservice.integrationTests.api.messengerService

import com.ninjasquad.springmockk.MockkBean
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserService
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceProlongationService
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.util.ddMMyyyyDateTimeFormatter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("integration-tests")
class MessengerInstanceProlongationServiceIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @MockkBean
    lateinit var keycloakUserService: KeycloakUserService

    @Autowired
    lateinit var messengerInstanceRepository: MessengerInstanceRepository

    @Autowired
    lateinit var messengerInstanceProlongationService: MessengerInstanceProlongationService

    private val telematikID: String = "1113-akq"
    private val professionOID: String = "1.2.276.0.76.4.53"
    private val username: String = "user"

    init {

        beforeEach {
            // check EoL date is set to 10 years after order date
            allInstancesEOLEquals(120) shouldBe true
        }

        this.describe("period of validity change by method call") {

            it("period of validity was extended, should change EoL date") {
                val periodOfValidityInMonths: Long = 132

                preparePeriodOfValidityChange(periodOfValidityInMonths)

                messengerInstanceProlongationService.alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase()

                allInstancesEOLEquals(periodOfValidityInMonths) shouldBe true
            }

            it("period of validity was reduced, should change EoL date") {
                val periodOfValidityInMonths: Long = 1

                preparePeriodOfValidityChange(1)

                messengerInstanceProlongationService.alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase()

                allInstancesEOLEquals(periodOfValidityInMonths) shouldBe true
            }

            it("period of validity was not changed, should not update instance") {
                val periodOfValidityInMonths: Long = 120

                preparePeriodOfValidityChange(periodOfValidityInMonths)

                messengerInstanceProlongationService.alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase()

                allInstancesEOLEquals(periodOfValidityInMonths) shouldBe true // should verify never call save after date check
            }

            it("date of order was changed, should change EoL date") {
                val periodOfValidityInMonths: Long = 120
                val dateOfOrder = "01.01.2025"

                preparePeriodOfValidityChange(periodOfValidityInMonths, dateOfOrder)

                messengerInstanceProlongationService.alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase()

                allInstancesDateOfOrderEquals(
                    LocalDate.parse(dateOfOrder, ddMMyyyyDateTimeFormatter)
                )
                allInstancesEOLEquals(120)
            }
        }

        this.describe("period of validity change by scheduled task") {
            it("period of validity was extended, should change EoL date automatically") {
                val periodOfValidityInMonths: Long = 132
                val dateOfOrder = "31.12.2025"

                preparePeriodOfValidityChange(periodOfValidityInMonths)

                await().atMost(10, TimeUnit.SECONDS).until {
                    allInstancesDateOfOrderEquals(
                        LocalDate.parse(dateOfOrder, ddMMyyyyDateTimeFormatter)
                    )
                    allInstancesEOLEquals(periodOfValidityInMonths)
                }
            }

            it("date of order was changed, should change date of order and EoL date automatically") {
                val periodOfValidityInMonths: Long = 10
                val dateOfOrder = "31.12.2020"

                preparePeriodOfValidityChange(periodOfValidityInMonths, dateOfOrder)

                await().atMost(10, TimeUnit.SECONDS).until {
                    allInstancesDateOfOrderEquals(
                        LocalDate.parse(dateOfOrder, ddMMyyyyDateTimeFormatter)
                    )
                    allInstancesEOLEquals(periodOfValidityInMonths)
                }
            }
        }
    }

    private fun allInstancesDateOfOrderEquals(date: LocalDate): Boolean {
        val instances =
            messengerInstanceRepository.findByUserIdAndTelematikIdAndProfessionId(username, telematikID, professionOID)
        return instances.all { it.dateOfOrder == date }
    }

    private fun allInstancesEOLEquals(periodOfValidityInMonths: Long): Boolean {
        val instances =
            messengerInstanceRepository.findByUserIdAndTelematikIdAndProfessionId(username, telematikID, professionOID)
        return instances.all { it.endDate == it.dateOfOrder.plusMonths(periodOfValidityInMonths) }
    }

    private fun preparePeriodOfValidityChange(periodOfValidityInMonths: Long, dateOfOrder: String = "15.03.2022") {
        val userAttributes = mapOf(
            "TelematikID" to listOf(telematikID),
            "ProfessionOID" to listOf(professionOID),
            "Laufzeit" to listOf("$periodOfValidityInMonths"), // set to new [periodOfValidityInMonths] testdata is initialized with 10 years
            "Bestelldatum" to listOf(dateOfOrder)
        )

        val userRepresentation = UserRepresentation()
        userRepresentation.username = username
        userRepresentation.attributes = userAttributes

        every { keycloakUserService.getEnabledOrderUsersWithAttributes() }.returns(listOf(userRepresentation))
    }
}