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

package de.akquinet.timref.registrationservice.integrationTests.api.messengerService

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceCreateService.Companion.X_HEADER_INSTANCE_RANDOM
import de.akquinet.timref.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import de.akquinet.timref.registrationservice.integrationTests.configuration.WiremockConfiguration
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstance
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.mockk
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(value = [IntegrationTestConfiguration::class, WiremockConfiguration::class])
class MessengerInstanceServiceControllerIT(
    @Qualifier("Rawdata") val rawdataWireMock: WireMockServer,
    @Qualifier("Operator") val operatorWireMock: WireMockServer,
    @Qualifier("VZD") val vzdWireMock: WireMockServer,
    @Qualifier("Keycloak") val keycloakWireMock: WireMockServer,
) : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var embeddedDatasource: DataSource

    private val telematikId = "telematikId"
    private val telematikIdLowercase: String = telematikId.lowercase()

    @PostConstruct
    fun migrateWithFlyway() {
        val flyway = Flyway.configure().dataSource(embeddedDatasource).locations("db/migration", "db/testdata-dev")
            .cleanOnValidationError(true).load()
        flyway.migrate()
    }

    init {

        beforeContainer {
            vzdWireMock.start()
            operatorWireMock.start()
            rawdataWireMock.start()
            keycloakWireMock.start()
        }

        afterContainer {
            vzdWireMock.stop()
            operatorWireMock.stop()
            rawdataWireMock.stop()
            keycloakWireMock.stop()
        }

        beforeEach {
            val javaWebToken = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "user")
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
                serverName = "proxy-test.eu.timref.akquinet.nx2.dev",
                publicBaseUrl = "proxy-test.eu.timref.akquinet.nx2.dev"
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
                val instanceRandom = result.response.getHeaderValue(X_HEADER_INSTANCE_RANDOM)
                val serverName = telematikIdLowercase + instanceRandom
                mockMvc.get("/messengerInstance/$serverName.localhost/admin")
                    .andDo { print() }
                    .andExpect { status { isCreated() } }
                mockMvc.delete("/messengerInstance/$serverName.localhost/")
                    .andDo { print() }
                    .andExpect { status { isOk() } }
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
                val instanceRandom = result.response.getHeaderValue(X_HEADER_INSTANCE_RANDOM)
                val serverName = telematikIdLowercase + instanceRandom
                mockMvc.get("/messengerInstance/$serverName.localhost/admin") {
                }
                    .andDo { print() }
                    .andExpect { status { isCreated() } }

                mockMvc.get("/messengerInstance/$serverName.localhost/admin") {
                }
                    .andDo { print() }
                    .andExpect { status { isConflict() } }


            }
            it("it should change logLevel") {
                mockMvc.post("/messengerInstance/${adminMessengerInstanceEntity.serverName}/loglevel") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(adminMessengerInstanceEntity.serverName)
                }
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }
        }
    }
}
