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

import com.github.tomakehurst.wiremock.WireMockServer
import com.ninjasquad.springmockk.SpykBean
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceCreateService.Companion.X_HEADER_INSTANCE_RANDOM
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceServiceImpl
import de.akquinet.timref.registrationservice.integrationTests.configuration.WiremockConfiguration
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.timref.registrationservice.persistance.orgAdmin.OrgAdminRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(WiremockConfiguration::class)
class MessengerInstanceServiceIT(
    @Qualifier("Rawdata") val rawdataWireMock: WireMockServer,
    @Qualifier("Operator") val operatorWireMock: WireMockServer,
    @Qualifier("VZD") val vzdWireMock: WireMockServer,
    @Qualifier("Keycloak") val keycloakWireMock: WireMockServer,
) : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @SpykBean
    lateinit var messengerInstanceService: MessengerInstanceServiceImpl

    @Autowired
    lateinit var messengerInstanceRepository: MessengerInstanceRepository

    @Autowired
    lateinit var orgAdminEntityRepository: OrgAdminRepository

    @Autowired
    lateinit var embeddedDatasource: DataSource

    @PostConstruct
    fun migrateWithFlyway() {
        val flyway = Flyway.configure().dataSource(embeddedDatasource).locations("db/migration")
            .cleanOnValidationError(true).load()
        flyway.migrate()
    }

    private val telematikId: String = "telematik.-Id"
    private val telematikIdLowercase: String = telematikId.lowercase()
    private val telematikIdLowercaseWithoutDots: String = telematikIdLowercase.replace(".", "")

    private fun setupAuthenticationInfos(runtime: Int, instances: Int) {
        val javaWebToken = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "user")
            .claim("telematik_id", telematikId)
            .claim("profession_oid", "professionOid")
            .claim("date_of_order", "15.03.2022")
            .claim("runtime", runtime.toString())
            .claim("instances", instances.toString())
            .build()
        val authentication = mockk<JwtAuthenticationToken>()
        val securityContext = mockk<SecurityContext>()
        every { securityContext.authentication } returns authentication
        every { securityContext.authentication.principal } returns javaWebToken
        SecurityContextHolder.setContext(securityContext)
    }

    init {

        val request: HttpServletRequest = mockk {
            every { getHeader("Content-Length") } returns  "13"
        }

        val dateOfOrder = LocalDate.of(2022, 3, 15)
        val endDate = dateOfOrder.plusMonths(120)

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

        this.describe("Messenger instance service") {

            beforeEach {
                setupAuthenticationInfos(120, 10)
            }

            afterEach {
                // clear repository
                orgAdminEntityRepository.deleteAll()
                messengerInstanceRepository.deleteAll()
            }

            it("creates a new messenger instance with correct fields") {
                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom
                    val instanceId = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        // also count instances fom testdata
                        it shouldHaveSize 1

                        val instance = it.last()
                        instance.serverName shouldBe "$serverName.localhost"
                        instance.publicBaseUrl shouldBe "$serverName.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId}localhost"
                        instance.dateOfOrder shouldBe dateOfOrder.toString()
                        instance.endDate shouldBe endDate.toString()
                    }
                }
            }

            it("deletes a messenger instance by servername") {
                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom

                    createAdminUser("$serverName.localhost").statusCode shouldBe HttpStatus.CREATED

                    deleteInstance("$serverName.localhost").statusCode shouldBe HttpStatus.OK
                }

            }

            it("deletes a messenger instance by servername without admin user") {
                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom

                    deleteInstance("$serverName.localhost").statusCode shouldBe HttpStatus.OK
                }

            }

            it("should create two messenger instances, get both, delete one and then get only the remaining one") {
                var serverName1: String
                val instanceId1: String
                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED
                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    serverName1 = telematikIdLowercase + instanceRandom
                    instanceId1 = telematikIdLowercaseWithoutDots + instanceRandom
                }

                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName2 = telematikIdLowercase + instanceRandom

                    createAdminUser("$serverName2.localhost").statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForUser().also {
                        it shouldHaveSize 2
                    }
                    deleteInstance("$serverName2.localhost").statusCode shouldBe HttpStatus.OK
                    getAllInstancesForUser().also {
                        it shouldHaveSize 1

                        val instance = it.single()
                        instance.serverName shouldBe "$serverName1.localhost"
                        instance.publicBaseUrl shouldBe "$serverName1.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId1}localhost"
                    }
                }

            }

            it("should not create a new messenger instance because the maximum runtime of the initial order is already ended") {
                setupAuthenticationInfos(12, 10)

                messengerInstanceService.run {
                    createNewInstance(
                        request
                    ).statusCode shouldBe HttpStatus.PRECONDITION_FAILED
                }
            }

            it("should create 3 new messenger instance but not the fourth") {
                setupAuthenticationInfos(120, 3)

                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom
                    val instanceId = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        it shouldHaveSize 1

                        val instance = it.single()
                        instance.serverName shouldBe "$serverName.localhost"
                        instance.publicBaseUrl shouldBe "$serverName.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId}localhost"
                    }
                }

                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom
                    val instanceId = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        it shouldHaveSize 2

                        val instance = it.last()
                        instance.serverName shouldBe "$serverName.localhost"
                        instance.publicBaseUrl shouldBe "$serverName.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId}localhost"
                    }
                }

                var serverName3: String?
                var instanceId3: String?

                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()

                    serverName3 = telematikIdLowercase + instanceRandom
                    instanceId3 = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.serverName shouldBe "$serverName3.localhost"
                        instance.publicBaseUrl shouldBe "$serverName3.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId3}localhost"
                    }
                }

                messengerInstanceService.run {
                    createNewInstance(request).statusCode shouldBe HttpStatus.FORBIDDEN
                    getAllInstancesForUser().also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.serverName shouldBe "$serverName3.localhost"
                        instance.publicBaseUrl shouldBe "$serverName3.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId3}localhost"
                    }
                }
            }

            it("should create 3 new messenger instance, delete the second and then create a fourth with telematikId004") {
                setupAuthenticationInfos(120, 3)

                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom
                    val instanceId = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        it shouldHaveSize 1

                        val instance = it.single()
                        instance.serverName shouldBe "$serverName.localhost"
                        instance.publicBaseUrl shouldBe "$serverName.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId}localhost"
                    }
                }

                var serverName2: String?
                var instanceId2: String?
                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    serverName2 = telematikIdLowercase + instanceRandom
                    instanceId2 = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        it shouldHaveSize 2

                        val instance = it.last()
                        instance.serverName shouldBe "$serverName2.localhost"
                        instance.publicBaseUrl shouldBe "$serverName2.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId2}localhost"
                    }
                }

                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom
                    val instanceId = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.serverName shouldBe "$serverName.localhost"
                        instance.publicBaseUrl shouldBe "$serverName.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId}localhost"
                    }
                }

                messengerInstanceService.run {
                    createAdminUser("$serverName2.localhost").statusCode shouldBe HttpStatus.CREATED

                    deleteInstance("$serverName2.localhost").statusCode shouldBe HttpStatus.OK
                }

                messengerInstanceService.run {
                    val response = createNewInstance(request)
                    response.statusCode shouldBe HttpStatus.CREATED

                    val instanceRandom = response.headers[X_HEADER_INSTANCE_RANDOM]?.first()
                    val serverName = telematikIdLowercase + instanceRandom
                    val instanceId = telematikIdLowercaseWithoutDots + instanceRandom

                    getAllInstancesForUser().also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.serverName shouldBe "$serverName.localhost"
                        instance.publicBaseUrl shouldBe "$serverName.localhost"
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionId shouldBe "professionOid"
                        instance.instanceId shouldBe "${instanceId}localhost"
                    }
                }
            }
        }
    }
}
