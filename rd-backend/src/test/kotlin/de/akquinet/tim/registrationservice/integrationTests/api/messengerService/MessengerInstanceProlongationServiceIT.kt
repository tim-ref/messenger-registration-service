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

package de.akquinet.tim.registrationservice.integrationTests.api.messengerService

import com.ninjasquad.springmockk.MockkBean
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserService
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceProlongationService
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.awaitility.Awaitility.await
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
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
            isEndOfLifeDateSetToOrderDatePlusPeriodOfValidity(120) shouldBe true
        }

        this.describe("period of validity change by method call") {

            it("period of validity was extended, should change EoL date") {
                val periodOfValidityInMonths: Long = 132

                preparePeriodOfValidityChange(periodOfValidityInMonths)

                messengerInstanceProlongationService.alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase()

                isEndOfLifeDateSetToOrderDatePlusPeriodOfValidity(periodOfValidityInMonths) shouldBe true
            }

            it("period of validity was reduced, should change EoL date"){
                val periodOfValidityInMonths: Long = 1

                preparePeriodOfValidityChange(1)

                messengerInstanceProlongationService.alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase()

                isEndOfLifeDateSetToOrderDatePlusPeriodOfValidity(periodOfValidityInMonths) shouldBe true
            }

            it("period of validity was not changed, should not update instance"){
                val periodOfValidityInMonths: Long = 120

                preparePeriodOfValidityChange(periodOfValidityInMonths)

                messengerInstanceProlongationService.alignOrderLengthFromKeycloakAttributeToRegistrationServiceDatabase()

                isEndOfLifeDateSetToOrderDatePlusPeriodOfValidity(periodOfValidityInMonths) shouldBe true // should verify never call save after date check
            }
        }

        this.describe("period of validity change by scheduled task"){
            it("period of validity was extended, should change EoL date automatically") {
                val periodOfValidityInMonths: Long = 132

                preparePeriodOfValidityChange(periodOfValidityInMonths)

                await().atMost(10, TimeUnit.SECONDS).until {
                    isEndOfLifeDateSetToOrderDatePlusPeriodOfValidity(periodOfValidityInMonths)
                }
            }
        }
    }

    private fun isEndOfLifeDateSetToOrderDatePlusPeriodOfValidity(periodOfValidityInMonths: Long):Boolean {
        messengerInstanceRepository.findByUserIdAndTelematikIdAndProfessionId(username, telematikID, professionOID)
            .let { instances ->
                instances.forEach {
                    if(it.endDate != it.dateOfOrder.plusMonths(periodOfValidityInMonths)) return false
                }
            }
        return true
    }

    private fun preparePeriodOfValidityChange(periodOfValidityInMonths: Long){
        val userAttributes = mapOf(
            Pair("TelematikID", listOf(telematikID)),
            Pair("ProfessionOID", listOf(professionOID)),
            Pair("Laufzeit", listOf("$periodOfValidityInMonths")) // set to new [periodOfValidityInMonths] testdata is initialized with 10 years
        )

        val userRepresentation = UserRepresentation()
        userRepresentation.username = username
        userRepresentation.attributes = userAttributes

        every { keycloakUserService.getEnabledMessengerInstanceOrderUsers() }.returns(listOf(userRepresentation))
    }
}