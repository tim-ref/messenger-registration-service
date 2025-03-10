/*
 * Copyright (C) 2025 akquinet GmbH
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

package de.akquinet.tim.registrationservice.integrationTests.api.wellknownsupport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import de.akquinet.tim.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import de.akquinet.tim.registrationservice.integrationTests.configuration.WiremockConfiguration
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ServerSupportInformation
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.mockk
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(value = [IntegrationTestConfiguration::class, WiremockConfiguration::class])
class WellKnownSupportControllerIT(
    @Qualifier("Keycloak") val keycloakWireMock: WireMockServer
) : DescribeSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    init {

        beforeContainer {
            keycloakWireMock.start()
        }

        afterContainer {
            keycloakWireMock.stop()
        }

        beforeEach {
            val javaWebToken = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "user")
                .claim("email", "user@instance.tim")
                .claim("telematik_id", "telematikId")
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

        this.describe("WellKnownSupportIT - complete API Endpoint "){

            it("store data for a server, should retrieve it"){
                val contentBody = jacksonObjectMapper().writeValueAsString(ServerSupportInformation(supportPage = "http://example.com"))

                mockMvc.put("/well-known-support/testServer"){
                    contentType = MediaType.APPLICATION_JSON
                    content = contentBody
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isOk() } }

                mockMvc.get("/well-known-support/testServer")
                    .andDo { print() }
                    .andExpect {
                        status { isOk() }
                        content { contentType(MediaType.APPLICATION_JSON) }
                        content { json("""{"support_page": "http://example.com", "contacts": []}""") }
                    }
            }

            it("store and update data for a server, should retrieve it"){
                var contentBody = jacksonObjectMapper().writeValueAsString(ServerSupportInformation(supportPage = "http://example.com"))

                mockMvc.put("/well-known-support/testServer"){
                    contentType = MediaType.APPLICATION_JSON
                    content = contentBody
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isOk() } }

                contentBody = jacksonObjectMapper().writeValueAsString(ServerSupportInformation(supportPage = "http://changed.example.com"))

                mockMvc.put("/well-known-support/testServer"){
                    contentType = MediaType.APPLICATION_JSON
                    content = contentBody
                    characterEncoding = Charsets.UTF_8.name()
                }
                    .andDo { print() }
                    .andExpect { status { isOk() } }

                mockMvc.get("/well-known-support/testServer")
                    .andDo { print() }
                    .andExpect {
                        status { isOk() }
                        content { contentType(MediaType.APPLICATION_JSON) }
                        content { json("""{"support_page": "http://changed.example.com", "contacts": []}""") }
                    }
            }
        }
    }
}