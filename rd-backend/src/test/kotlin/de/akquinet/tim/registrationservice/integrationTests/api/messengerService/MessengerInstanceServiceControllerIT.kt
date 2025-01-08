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
import com.google.gson.Gson
import com.ninjasquad.springmockk.SpykBean
import de.akquinet.tim.registrationservice.api.messengerproxy.MessengerProxyLogLevelService
import de.akquinet.tim.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import de.akquinet.tim.registrationservice.integrationTests.configuration.WiremockConfiguration
import de.akquinet.tim.registrationservice.openapi.api.operator.client.SynapseOperatorApi
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateAdminUserRequest
import de.akquinet.tim.registrationservice.openapi.model.mi.MessengerInstanceDto
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
import org.springframework.http.ResponseEntity
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

    @SpykBean
    lateinit var messengerProxyLogLevelService: MessengerProxyLogLevelService

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
            val adminMessengerInstanceDto = MessengerInstanceDto(
                instanceId = "proxytesteu",
                instanceName = "proxy-test.eu.timref.akquinet.nx2.dev",
                publicHomeserverFQDN = "proxy-test.eu.timref.akquinet.nx2.dev"
            )

            it("should get all already created messenger instances") {
                mockMvc.get("/messenger-instances")
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }

            it("should create a new messenger instance") {
                mockMvc.post("/messenger-instance/request") {}
                    .andDo { print() }
                    .andExpect { status { isAccepted() } }
            }
            xit("it should create an admin user") {
                mockMvc.post("/messenger-instance/request/admin") {
                    contentType = MediaType.APPLICATION_JSON
                    content = Gson().toJson(CreateAdminUserRequest(instanceName = adminMessengerInstanceDto.instanceId))
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isCreated() } }
            }

            xit("it should create and delete a new messenger instance") {
                val result = mockMvc.post("/messenger-instance/request") {}
                    .andDo { print() }
                    .andExpect { status { isAccepted() } }
                    .andReturn()
                val instanceId = result.response.getHeaderValue("X_HEADER_INSTANCE_RANDOM")

                mockMvc.post("/messenger-instance/request/admin") {
                    contentType = MediaType.APPLICATION_JSON
                    content = Gson().toJson(CreateAdminUserRequest(instanceName = "$instanceId"))
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isCreated() } }


                mockMvc.delete("/messenger-instance/$instanceId/")
                    .andDo { print() }
                    .andExpect { status { isNoContent() } }
            }

            it("should create two messenger instances and list them") {
                mockMvc.post("/messenger-instance/request") {}
                    .andDo { print() }
                    .andExpect { status { isAccepted() } }

                mockMvc.post("/messenger-instance/request") {}
                    .andDo { print() }
                    .andExpect { status { isAccepted() } }

                mockMvc.get("/messenger-instances")
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }

            it("it should fail to create an admin user for non existent server") {
                mockMvc.post("/messenger-instance/request/admin") {
                    contentType = MediaType.APPLICATION_JSON
                    content = Gson().toJson(CreateAdminUserRequest(instanceName = "not-existing-server"))
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isLocked() } }
            }

            xit("it should fail to create a second admin user") {
                val result = mockMvc.post("/messenger-instance/request") {}
                    .andDo { print() }
                    .andExpect { status { isAccepted() } }
                    .andReturn()
                val instanceId = result.response.getHeaderValue("X_HEADER_INSTANCE_RANDOM")
                mockMvc.post("/messenger-instance/request/admin") {
                    contentType = MediaType.APPLICATION_JSON
                    content = Gson().toJson(CreateAdminUserRequest(instanceName = "$instanceId"))
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isCreated() } }

                mockMvc.post("/messenger-instance/request/admin") {
                    contentType = MediaType.APPLICATION_JSON
                    content = Gson().toJson(CreateAdminUserRequest(instanceName = "$instanceId"))
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isConflict() } }
            }

            it("it should change logLevel") {
                val newLogLevel = LogLevel.DEBUG
                every {
                    messengerProxyLogLevelService.changeLogLevel(any(), any())
                } returns ResponseEntity.ok().build()

                mockMvc.post("/logging/${adminMessengerInstanceDto.instanceId}/level") {
                    contentType = MediaType.TEXT_PLAIN
                    content = newLogLevel.name
                }
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }
        }
    }
}
