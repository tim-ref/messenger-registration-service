/*
 * Copyright (C) 2023 - 2024 akquinet GmbH
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

import com.github.tomakehurst.wiremock.WireMockServer
import com.ninjasquad.springmockk.SpykBean
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceCreateService.Companion.X_HEADER_INSTANCE_RANDOM
import de.akquinet.tim.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import de.akquinet.tim.registrationservice.integrationTests.configuration.WiremockConfiguration
import de.akquinet.tim.registrationservice.openapi.operator.client.SynapseOperatorApi
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstance
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.mockk
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.openapitools.client.infrastructure.Success
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(value = [IntegrationTestConfiguration::class, WiremockConfiguration::class])
class MessengerInstanceServiceControllerIT(
    @Qualifier("Rawdata") val rawdataWireMock: WireMockServer,
    @Qualifier("Operator") val operatorWireMock: WireMockServer,
    @Qualifier("VZD") val vzdWireMock: WireMockServer,
    @Qualifier("Keycloak") val keycloakWireMock: WireMockServer,
    @Qualifier("Proxy") val messengerProxyWireMock: WireMockServer,
) : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @SpykBean
    lateinit var operatorApiClient: SynapseOperatorApi

    private val telematikId = "telematikId"

    init {

        beforeContainer {
            vzdWireMock.start()
            operatorWireMock.start()
            rawdataWireMock.start()
            keycloakWireMock.start()
            messengerProxyWireMock.start()
        }

        afterContainer {
            vzdWireMock.stop()
            operatorWireMock.stop()
            rawdataWireMock.stop()
            keycloakWireMock.stop()
            messengerProxyWireMock.stop()
        }

        beforeEach {
            val javaWebToken = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "user")
                .claim("email", "user@instance.tim")
                .claim("telematik_id", telematikId)
                .claim("profession_oid", "professionOid")
                .claim("date_of_order", "15.03.2023")
                .claim("runtime", "120")
                .claim("instances", "10")
                .build()
            val authentication = mockk<JwtAuthenticationToken>()
            val securityContext = mockk<SecurityContext>()
            every { securityContext.authentication } returns authentication
            every { securityContext.authentication.principal } returns javaWebToken
            SecurityContextHolder.setContext(securityContext)
        }

        this.describe("Messenger Instance Controller") {

            val adminMessengerInstanceEntity = MessengerInstance(
                serverName = "proxy-test.timref.example.com",
                publicBaseUrl = "proxy-test.timref.example.com"
            )

            it("should get all already created messenger instances") {
                mockMvc.get("/messengerInstance")
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }

            it("should create a new messenger instance") {
                mockMvc.post("/messengerInstance/create") {}
                    .andDo { print() }
                    .andExpect { status { isCreated() } }
            }
            it("it should create an admin user") {
                mockMvc.get("/messengerInstance/${adminMessengerInstanceEntity.serverName}/admin") {
                }
                    .andDo { print() }
                    .andExpect { status { isCreated() } }
            }

            it("it should create and delete a new messenger instance") {
                val result = mockMvc.post("/messengerInstance/create") {}
                    .andDo { print() }
                    .andExpect { status { isCreated() } }
                    .andReturn()
                val instanceName = result.response.getHeaderValue(X_HEADER_INSTANCE_RANDOM)
                mockMvc.get("/messengerInstance/$instanceName.localhost/admin")
                    .andDo { print() }
                    .andExpect { status { isCreated() } }
                mockMvc.delete("/messengerInstance/$instanceName.localhost/")
                    .andDo { print() }
                    .andExpect { status { isNoContent() } }
            }

            it("should create two messenger instances and list them") {
                mockMvc.post("/messengerInstance/create") {}
                    .andDo { print() }
                    .andExpect { status { isCreated() } }

                mockMvc.post("/messengerInstance/create") {}
                    .andDo { print() }
                    .andExpect { status { isCreated() } }

                mockMvc.get("/messengerInstance")
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }

            it("it should fail to create an admin user for non existent server") {
                mockMvc.get("/messengerInstance/NotExistingServer/admin") {
                }
                    .andDo { print() }
                    .andExpect { status { isLocked() } }
            }

            it("it should fail to create a second admin user") {
                val result = mockMvc.post("/messengerInstance/create") {}
                    .andDo { print() }
                    .andExpect { status { isCreated() } }
                    .andReturn()
                val instanceName = result.response.getHeaderValue(X_HEADER_INSTANCE_RANDOM)
                mockMvc.get("/messengerInstance/$instanceName.localhost/admin") {
                }
                    .andDo { print() }
                    .andExpect { status { isCreated() } }

                mockMvc.get("/messengerInstance/$instanceName.localhost/admin") {
                }
                    .andDo { print() }
                    .andExpect { status { isConflict() } }
            }

            it("it should change logLevel") {
                val newLogLevel = LogLevel.DEBUG
                every {
                    operatorApiClient.changeMessengerInstanceLoglevelWithHttpInfo(any(), any())
                }.returns(Success(data = null, statusCode = 201))
                mockMvc.post("/messengerInstance/${adminMessengerInstanceEntity.serverName}/loglevel") {
                    contentType = MediaType.APPLICATION_JSON
                    content = newLogLevel.name
                }
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }
        }
    }
}
