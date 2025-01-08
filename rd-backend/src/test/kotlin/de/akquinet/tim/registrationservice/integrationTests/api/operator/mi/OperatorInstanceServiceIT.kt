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

package de.akquinet.tim.registrationservice.integrationTests.api.operator.mi

import com.github.tomakehurst.wiremock.WireMockServer
import com.ninjasquad.springmockk.SpykBean
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserService
import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceService
import de.akquinet.tim.registrationservice.api.messengerservice.OperatorInstanceCreateService
import de.akquinet.tim.registrationservice.api.messengerservice.OperatorInstanceDeleteService
import de.akquinet.tim.registrationservice.integrationTests.configuration.WiremockConfiguration
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateAdminUserRequest
import de.akquinet.tim.registrationservice.openapi.model.mi.CreateMessengerInstanceRequest
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.persistance.orgAdmin.OrgAdminRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests-without-testdata")
@Import(WiremockConfiguration::class)
class OperatorInstanceServiceIT(
    @Qualifier("Rawdata") val rawdataWireMock: WireMockServer,
    @Qualifier("Operator") val operatorWireMock: WireMockServer,
    @Qualifier("VZD") val vzdWireMock: WireMockServer,
    @Qualifier("Keycloak") val keycloakWireMock: WireMockServer,
) : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @SpykBean
    lateinit var messengerInstanceService: MessengerInstanceService

    @SpykBean
    lateinit var operatorInstanceCreateService: OperatorInstanceCreateService

    @SpykBean
    lateinit var operatorInstanceDeleteService: OperatorInstanceDeleteService

    @Autowired
    lateinit var messengerInstanceRepository: MessengerInstanceRepository

    @Autowired
    lateinit var orgAdminEntityRepository: OrgAdminRepository

    @Autowired
    lateinit var keycloakUserService: KeycloakUserService

    private val telematikId: String = "telematik.-Id"

    private fun setupAuthenticationInfosBasicAuth() {
        val user: UserDetails = User.builder()
            .username("operator")
            .password("operator")
            .roles("OPERATOR")
            .build()
        val authentication = mockk<UsernamePasswordAuthenticationToken>()
        val securityContext = mockk<SecurityContext>()
        every { securityContext.authentication } returns authentication
        every { securityContext.authentication.principal } returns user
        SecurityContextHolder.setContext(securityContext)
    }

    private fun setupAuthenticationInfosJwt(runtime: Int, instances: Int) {
        val javaWebToken = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "user")
            .claim("telematik_id", telematikId)
            .claim("email", "user@instance.tim")
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

        val dateOfOrder = LocalDate.now()
        val endDate = dateOfOrder.plusYears(100)

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
            afterEach {
                // clear repository
                orgAdminEntityRepository.deleteAll()
                messengerInstanceRepository.deleteAll()
            }

            val createRequestParams = CreateMessengerInstanceRequest(
                instanceName = "myMessengerInstance",
                publicHomeserverFQDN = "myMessengerInstance.local",
                telematikId = telematikId,
                professionOid = "professionOid",
                userId = "user"
            )

            val createRequestParams2 = CreateMessengerInstanceRequest(
                instanceName = "myMessengerInstance2",
                publicHomeserverFQDN = "myMessengerInstance2.local",
                telematikId = telematikId,
                professionOid = "professionOid",
                userId = "user"
            )

            val createRequestParams3 = CreateMessengerInstanceRequest(
                instanceName = "myMessengerInstance3",
                publicHomeserverFQDN = "myMessengerInstance3.local",
                telematikId = telematikId,
                professionOid = "professionOid",
                userId = "user"
            )

            val createRequestParams4 = CreateMessengerInstanceRequest(
                instanceName = "myMessengerInstance4",
                publicHomeserverFQDN = "myMessengerInstance4.local",
                telematikId = telematikId,
                professionOid = "professionOid",
                userId = "user"
            )

            it("creates a new messenger instance with correct fields") {
                setupAuthenticationInfosBasicAuth()
                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(
                        request = request,
                        createRequestParams = createRequestParams
                    )
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams.userId).also {
                        it shouldHaveSize 1

                        val instance = it.last()
                        instance.instanceName shouldBe createRequestParams.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                        instance.dateOfOrder shouldBe dateOfOrder
                        instance.endDate shouldBe endDate
                    }
                }
            }

            it("deletes a messenger instance by servername") {
                setupAuthenticationInfosBasicAuth()
                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(
                        request = request,
                        createRequestParams = createRequestParams
                    )
                    response.statusCode shouldBe HttpStatus.CREATED

                    keycloakUserService.createAdminUser(
                        CreateAdminUserRequest(
                            instanceName = createRequestParams.instanceName,
                            orgAdminEmailAddress = "mail@example.com"
                        )
                    ).statusCode shouldBe HttpStatus.CREATED

                    operatorInstanceDeleteService.deleteInstance(createRequestParams.instanceName).statusCode shouldBe HttpStatus.NO_CONTENT
                }
            }

            it("deletes a messenger instance by servername without admin user") {
                setupAuthenticationInfosBasicAuth()
                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(
                        request = request,
                        createRequestParams = createRequestParams
                    )
                    response.statusCode shouldBe HttpStatus.CREATED

                    operatorInstanceDeleteService.deleteInstance(createRequestParams.instanceName).statusCode shouldBe HttpStatus.NO_CONTENT
                }
            }

            it("should create two messenger instances, get both, delete one and then get only the remaining one") {
                setupAuthenticationInfosBasicAuth()
                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(
                        request = request,
                        createRequestParams = createRequestParams
                    )
                    response.statusCode shouldBe HttpStatus.CREATED
                }

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(
                        request = request,
                        createRequestParams = createRequestParams2
                    )
                    response.statusCode shouldBe HttpStatus.CREATED

                    keycloakUserService.createAdminUser(CreateAdminUserRequest(createRequestParams2.instanceName)).statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams2.userId) shouldHaveSize 2

                    operatorInstanceDeleteService.deleteInstance(createRequestParams2.instanceName).statusCode shouldBe HttpStatus.NO_CONTENT
                    getAllInstancesForCurrentUser(createRequestParams.userId).also {
                        it shouldHaveSize 1

                        val instance = it.single()
                        instance.instanceName shouldBe createRequestParams.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }
            }

            it("should not create a new messenger instance because the maximum runtime of the initial order is already ended") {
                setupAuthenticationInfosJwt(12, 10)

                messengerInstanceService.run {
                    messengerInstanceService.requestNewInstance().statusCode shouldBe HttpStatus.PAYMENT_REQUIRED
                }
            }

            it("should create 3 new messenger instance but not the fourth") {
                setupAuthenticationInfosJwt(120, 3)

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(request, createRequestParams)
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams.userId).also {
                        it shouldHaveSize 1

                        val instance = it.single()
                        instance.instanceName shouldBe createRequestParams.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(request, createRequestParams2)
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams2.userId).also {
                        it shouldHaveSize 2

                        val instance = it.last()
                        instance.instanceName shouldBe createRequestParams2.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams2.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(request, createRequestParams3)
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams3.userId).also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.instanceName shouldBe createRequestParams3.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams3.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }

                messengerInstanceService.run {
                    messengerInstanceService.requestNewInstance().statusCode shouldBe HttpStatus.PAYMENT_REQUIRED
                    getAllInstancesForCurrentUser(createRequestParams3.userId).also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.instanceName shouldBe createRequestParams3.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams3.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }
            }

            it("should create 3 new messenger instance, delete the second and then create a fourth with telematikId004") {

                setupAuthenticationInfosJwt(120, 3)

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(request, createRequestParams)
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams.userId).also {
                        it shouldHaveSize 1

                        val instance = it.single()
                        instance.instanceName shouldBe createRequestParams.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(request, createRequestParams2)
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams2.userId).also {
                        it shouldHaveSize 2

                        val instance = it.last()
                        instance.instanceName shouldBe createRequestParams2.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams2.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(request, createRequestParams3)
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams3.userId).also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.instanceName shouldBe createRequestParams3.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams3.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }

                messengerInstanceService.run {
                    keycloakUserService.createAdminUser(
                        request = CreateAdminUserRequest(
                            instanceName = createRequestParams2.instanceName,
                            orgAdminEmailAddress = "me@you.com"
                        )
                    ).statusCode shouldBe HttpStatus.CREATED

                    operatorInstanceDeleteService.deleteInstance(createRequestParams2.instanceName).statusCode shouldBe HttpStatus.NO_CONTENT
                }

                messengerInstanceService.run {
                    val response = operatorInstanceCreateService.createNewInstance(request, createRequestParams4)
                    response.statusCode shouldBe HttpStatus.CREATED

                    getAllInstancesForCurrentUser(createRequestParams4.userId).also {
                        it shouldHaveSize 3

                        val instance = it.last()
                        instance.instanceName shouldBe createRequestParams4.instanceName
                        instance.publicHomeserverFQDN shouldBe createRequestParams4.publicHomeserverFQDN
                        instance.id.shouldNotBeNull()
                        instance.telematikId shouldBe telematikId
                        instance.professionOid shouldBe "professionOid"
                        instance.instanceId shouldNotBe null
                    }
                }
            }
        }
    }
}
