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

package de.akquinet.tim.registrationservice.unitTests.api.operator

import de.akquinet.tim.registrationservice.openapi.api.operator.client.SynapseOperatorApi
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfiguration
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfigurationProxy
import de.akquinet.tim.registrationservice.api.operator.OperatorService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.openapitools.client.infrastructure.Success
import org.springframework.http.HttpStatus

class OperatorServiceTest : DescribeSpec() {

    private val operatorApiClient: SynapseOperatorApi = mockk()
    private val operatorService = OperatorService(
        logger = mockk(relaxed = true),
        operatorConfig = mockk(relaxed = true),
        regServiceConfig = mockk(relaxed = true) {
            every { callExternalServices } returns true
        },
        operatorApi = operatorApiClient
    )

    init {
        describe("OperatorService") {

            it("should set auth concept config and return OK") {
                val instanceName = "testInstance"
                val authConceptConfig = SynapseOverrideConfigurationProxy(
                    federationCheckConcept = SynapseOverrideConfigurationProxy.FederationCheckConcept.PROXY,
                    inviteRejectionPolicy = SynapseOverrideConfigurationProxy.InviteRejectionPolicy.ALLOW_ALL
                )

                every {
                    operatorApiClient.changeMessengerInstanceConfigurationWithHttpInfo(
                        instanceName, any()
                    )
                } returns Success(null)

                val result = operatorService.setAuthConceptConfig(instanceName, authConceptConfig)

                result shouldBe HttpStatus.OK

                verify(exactly = 1) {
                    operatorApiClient.changeMessengerInstanceConfigurationWithHttpInfo(
                        instanceName, any()
                    )
                }
            }

            it("should return internal server error when setting auth concept config fails") {
                val instanceName = "testInstance"
                val authConceptConfig = SynapseOverrideConfigurationProxy(
                    federationCheckConcept = SynapseOverrideConfigurationProxy.FederationCheckConcept.PROXY,
                    inviteRejectionPolicy = SynapseOverrideConfigurationProxy.InviteRejectionPolicy.ALLOW_ALL
                )

                every {
                    operatorApiClient.changeMessengerInstanceConfigurationWithHttpInfo(any(), any())
                } throws RuntimeException("Error during API call")

                val result = operatorService.setAuthConceptConfig(instanceName, authConceptConfig)

                result shouldBe HttpStatus.INTERNAL_SERVER_ERROR
            }

            it("should get auth concept config and return OK") {
                val instanceName = "testInstance"
                val authConceptConfig = SynapseOverrideConfiguration(
                    proxy = SynapseOverrideConfigurationProxy(
                        federationCheckConcept = SynapseOverrideConfigurationProxy.FederationCheckConcept.PROXY,
                        inviteRejectionPolicy = SynapseOverrideConfigurationProxy.InviteRejectionPolicy.ALLOW_ALL
                    )
                )

                every {
                    operatorApiClient.getMessengerInstanceConfigurationWithHttpInfo(instanceName)
                } returns  Success(authConceptConfig)

                val result = operatorService.getAuthConceptConfig(instanceName)

                result.statusCode shouldBe HttpStatus.OK
                result.body?.federationCheckConcept shouldBe SynapseOverrideConfigurationProxy.FederationCheckConcept.PROXY
                result.body?.inviteRejectionPolicy shouldBe SynapseOverrideConfigurationProxy.InviteRejectionPolicy.ALLOW_ALL
            }

            it("should return internal server error when getting auth concept config fails") {
                val instanceName = "testInstance"

                every {
                    operatorApiClient.getMessengerInstanceConfigurationWithHttpInfo(instanceName)
                } throws RuntimeException("Error during API call")

                val result = operatorService.getAuthConceptConfig(instanceName)

                result.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                result.body shouldBe null
            }
        }
    }
}
