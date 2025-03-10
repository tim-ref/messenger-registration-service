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

package de.akquinet.tim.registrationservice.unitTests.api.wellknownsupport

import de.akquinet.tim.registrationservice.api.wellknownsupport.NotAllowedException
import de.akquinet.tim.registrationservice.api.wellknownsupport.WellKnownSupportService
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.Contact
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ContactRole
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ServerSupportInformation
import de.akquinet.tim.registrationservice.persistance.wellKnownSupport.SupportInformationRepository
import de.akquinet.tim.registrationservice.persistance.wellKnownSupport.model.SupportContactEntity
import de.akquinet.tim.registrationservice.persistance.wellKnownSupport.model.SupportInformationEntity
import de.akquinet.tim.registrationservice.util.NotLoggedInException
import de.akquinet.tim.registrationservice.util.UserService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class WellKnownSupportServiceTest : DescribeSpec() {

    private lateinit var supportInfoRepository: SupportInformationRepository
    private lateinit var userService: UserService
    private lateinit var wellKnownSupportService: WellKnownSupportService

    init {
        beforeTest {
            supportInfoRepository = mockk(relaxed = true)
            userService = mockk(relaxed = true)
            wellKnownSupportService = WellKnownSupportService(
                supportInfoRepository = supportInfoRepository,
                userService = userService,
            )
        }

        describe("WellKnownSupportService"){

            // get
            it("available support info for given serverName, should return support info"){
                every { supportInfoRepository.findByServerName("givenServer") } returns SupportInformationEntity(
                    id = UUID.randomUUID(),
                    userId = "userId",
                    supportPage = "http://example.com",
                    serverName = "givenServer",
                    contacts = listOf(
                        SupportContactEntity(
                            id = UUID.randomUUID(),
                            emailAddress = "support@example.com",
                            matrixId = "@support:givenServer",
                            role = ContactRole.admin.value
                        )
                    )
                )

                val result = wellKnownSupportService.getSupportInformationForServerName("givenServer")

                result shouldBe ServerSupportInformation(
                    supportPage = "http://example.com",
                    contacts = listOf(
                        Contact(
                            emailAddress = "support@example.com",
                            matrixId = "@support:givenServer",
                            role = ContactRole.admin
                        )
                    )
                )
            }

            it("unavailable support info for given serverName, should return null"){
                every { supportInfoRepository.findByServerName(any()) } returns null

                val result = wellKnownSupportService.getSupportInformationForServerName("unavailable")

                result shouldBe null
            }

            // store
            it("authenticated user with valid data, should store the data and return true"){
                every { userService.getUserIdFromContext() } returns "givenUserId"
                every { supportInfoRepository.findByServerName("notGivenServer") } returns null
                every { supportInfoRepository.save(any()) } returns SupportInformationEntity(
                    id = UUID.randomUUID(),
                    serverName = "notGivenServer",
                    userId = "givenUserId",
                    supportPage = "http://example.com"
                )

                wellKnownSupportService.setSupportInformationForServerName(
                    serverName = "notGivenServer",
                    supportInformation = ServerSupportInformation(
                        supportPage = "http://example.com",
                        contacts = listOf()
                    )
                )

                verify { supportInfoRepository.save(any()) }
            }

            it("given valid data with available data for serverName, should update the data and return true"){
                val supportInfoId = UUID.randomUUID()
                val updatedEntity = SupportInformationEntity(
                    id = supportInfoId,
                    userId = "userId",
                    serverName = "givenServer",
                    supportPage = "http://example.com",
                    contacts = listOf()
                )
                every { supportInfoRepository.findByServerName("givenServer") } returns SupportInformationEntity(
                    id = supportInfoId,
                    userId = "userId",
                    serverName = "givenServer",
                    supportPage = null,
                    contacts = listOf(
                        SupportContactEntity(
                            id = UUID.randomUUID(),
                            emailAddress = "support@example.com",
                            matrixId = "@support:givenServer",
                            role = ContactRole.security.value
                        )
                    )
                )
                every { supportInfoRepository.save(any()) } returns updatedEntity

                wellKnownSupportService.setSupportInformationForServerName(
                    serverName = "givenServer",
                    supportInformation = ServerSupportInformation(
                        supportPage = "http://example.com",
                        contacts = listOf()
                    ),
                    userId = "userId"
                )

                verify { supportInfoRepository.save(any()) }
            }

            it("unauthenticated without userId, should throw NotLoggedInException"){
                every { userService.getUserIdFromContext() } throws NotLoggedInException()

                shouldThrow<NotLoggedInException> {
                    wellKnownSupportService.setSupportInformationForServerName(
                        serverName = "givenServer",
                        supportInformation = ServerSupportInformation(
                            supportPage = "http://example.com"
                        )
                    )
                }

                verify { supportInfoRepository.save(any()) wasNot called }
            }

            it("try change existing data for somebody else's server, should throw NotAllowedException"){
                every { supportInfoRepository.findByServerName("givenServer") } returns SupportInformationEntity(
                    id = UUID.randomUUID(),
                    userId = "userId",
                    serverName = "givenServer",
                    supportPage = null,
                    contacts = listOf(
                        SupportContactEntity(
                            id = UUID.randomUUID(),
                            emailAddress = "support@example.com",
                            matrixId = "@support:givenServer",
                            role = ContactRole.admin.value
                        )
                    )
                )

                shouldThrow<NotAllowedException> {
                    wellKnownSupportService.setSupportInformationForServerName(
                        userId = "someoneElseUserId",
                        serverName = "givenServer",
                        supportInformation = ServerSupportInformation(
                            supportPage = "http://example.com"
                        )
                    )
                }

                verify { supportInfoRepository.save(any()) wasNot called }
            }

            it("given empty contacts and supportPage, should throw IllegalArgumentException"){
                val exception = shouldThrow<IllegalArgumentException> {
                    wellKnownSupportService.setSupportInformationForServerName(
                        serverName = "givenServer",
                        supportInformation = ServerSupportInformation(
                            supportPage = null,
                            contacts = listOf()
                        ),
                        userId = "userId"
                    )
                }
                exception.localizedMessage shouldContainIgnoringCase "[supportPage] und [contacts] dürfen nicht beide leer sein"

                verify { supportInfoRepository.save(any()) wasNot called }
            }

            it("given empty serverName, should throw IllegalArgumentException"){
                val exception = shouldThrow<IllegalArgumentException> {
                    wellKnownSupportService.setSupportInformationForServerName(
                        serverName = "",
                        supportInformation = ServerSupportInformation(
                            supportPage = "anyString",
                            contacts = listOf()
                        ),
                        userId = "userId"
                    )
                }

                exception.localizedMessage shouldContainIgnoringCase "[serverName] darf nicht leer sein."

                verify { supportInfoRepository.save(any()) wasNot called }
            }

            it("given contact with empty dmailAddress and emptymatrixId, should throw IllegalArgumentException"){
                val exception = shouldThrow<IllegalArgumentException> {
                    wellKnownSupportService.setSupportInformationForServerName(
                        serverName = "givenServer",
                        supportInformation = ServerSupportInformation(
                            supportPage = "anyString",
                            contacts = listOf(Contact(
                                role = ContactRole.security
                            ))
                        ),
                        userId = "userId"
                    )
                }

                exception.localizedMessage shouldContainIgnoringCase "[emailAddress] und [matrixId] dürfen nicht beide leer sein."

                verify { supportInfoRepository.save(any()) wasNot called }
            }
        }
    }
}