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

package de.akquinet.timref.registrationservice.integrationTests.api.invitePermission

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import de.akquinet.timref.registrationservice.api.federation.FederationServiceImpl
import de.akquinet.timref.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(IntegrationTestConfiguration::class)
class InvitePermissionControllerIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var embeddedDatasource: DataSource

    @MockkBean
    final lateinit var federationServiceImpl: FederationServiceImpl

    @MockkBean
    final lateinit var mockConnection: HttpURLConnection

    @PostConstruct
    fun migrateWithFlyway() {
        val flyway = Flyway.configure().dataSource(embeddedDatasource).locations("db/migration", "db/testdata-dev")
            .cleanOnValidationError(true).load()
        flyway.migrate()
    }

    init {
        this.describe("Invite permission controller test") {
            val inviter = "Inviter"
            val invited = "Receiver"

            // whenever must be called before each test, otherwise there is a NullPointerException
            beforeTest {
                every { federationServiceImpl.connectToVzd(
                    any<String>(),
                    any<Map<String, Any>>()
                )
                } returns mockConnection
            }

            it("should response with HttpStatus.OK when everything is fine") {
                // using answers to return a new byte stream every time
                every { mockConnection.inputStream } answers { ByteArrayInputStream("pract".toByteArray()) }

                mockMvc.post("/vzd/invite") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(Pair(inviter, invited))
                }
                    .andDo { print() }
                    .andExpect { status { isOk() } }
            }

            it("should response with HttpStatus.FORBIDDEN when inviter cannot invite invited") {
                // using answers to return a new byte stream every time
                every { mockConnection.inputStream } answers { ByteArrayInputStream("none".toByteArray()) }

                mockMvc.post("/vzd/invite") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(Pair(inviter, invited))
                }
                    .andDo { print() }
                    .andExpect { status { isForbidden() } }
            }
        }
    }
}
