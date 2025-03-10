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

import de.akquinet.tim.registrationservice.api.delegate.WellKnownSupportApiDelegateImpl
import de.akquinet.tim.registrationservice.api.wellknownsupport.NotAllowedException
import de.akquinet.tim.registrationservice.api.wellknownsupport.WellKnownSupportService
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.Contact
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ContactRole
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ServerSupportInformation
import de.akquinet.tim.registrationservice.util.NotLoggedInException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus

class WellKnownSupportApiTest : DescribeSpec() {
    private lateinit var service: WellKnownSupportService
    private lateinit var api: WellKnownSupportApiDelegateImpl

    init {
        beforeTest {
            service = mockk(relaxed = true)
            api = WellKnownSupportApiDelegateImpl(
                logger = mockk(relaxed = true),
                wellKnownSupportService = service
            )
        }

        describe("WellKnownSupportApi"){

            describe("Get support information") {

                it("data available, should return supportinfo") {
                    val expectedSupportInfo = ServerSupportInformation(
                        supportPage = "http://example.com",
                        contacts = listOf(
                            Contact(
                                role = ContactRole.admin,
                                emailAddress = "support@example.com",
                                matrixId = "@support:givenServer"
                            ),
                            Contact(
                                role = ContactRole.security,
                                emailAddress = "admins@example.com"
                            )
                        )
                    )

                    every { service.getSupportInformationForServerName("givenServer") } returns expectedSupportInfo

                    val result = api.retrieveSupportInformation("givenServer")

                    result.statusCode shouldBe HttpStatus.OK
                    result.body shouldBe expectedSupportInfo
                }

                it("unavailable data, should return NotFoundError") {
                    every { service.getSupportInformationForServerName("unavailable") } returns null

                    val result = api.retrieveSupportInformation("unavailable")

                    result.statusCode shouldBe HttpStatus.NOT_FOUND
                }

                it("an error, should return InternalServerError") {
                    every { service.getSupportInformationForServerName(any()) } throws IllegalArgumentException()

                    val result = api.retrieveSupportInformation("test")

                    result.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                }
            }

            describe("PUT support information"){

                it("send authenticated with correct data, should return OK"){
                    every {
                        service.setSupportInformationForServerName(
                            serverName = any(),
                            supportInformation = any()
                        )
                    } returns Unit

                    val result = api.setSupportInformation("test", ServerSupportInformation(supportPage = "test.page"))

                    result.statusCode shouldBe HttpStatus.OK
                }

                it("send unauthenticated, should return Unauthenticated"){
                    every {
                        service.setSupportInformationForServerName(
                            serverName = any(),
                            supportInformation = any()
                        )
                    } throws NotLoggedInException()

                    val result = api.setSupportInformation("test", ServerSupportInformation(supportPage = "test.page"))

                    result.statusCode shouldBe HttpStatus.UNAUTHORIZED
                }

                it("send incorrect data, should return BadRequest"){
                    every { service.setSupportInformationForServerName(
                        serverName = any(),
                        supportInformation = any()
                    ) } throws IllegalArgumentException()

                    val result = api.setSupportInformation("", ServerSupportInformation())

                    result.statusCode shouldBe HttpStatus.BAD_REQUEST
                }

                it("try update server from other userid, should return Forbidden"){
                    every { service.setSupportInformationForServerName(
                        serverName = any(),
                        supportInformation = any()
                    ) } throws NotAllowedException("error")

                    val result = api.setSupportInformation("other", ServerSupportInformation(supportPage = "test.page"))

                    result.statusCode shouldBe HttpStatus.FORBIDDEN
                }

                it("an unknown error occured, should return InternalServerError"){
                    every { service.setSupportInformationForServerName(
                        serverName = any(),
                        supportInformation = any()
                    ) } throws RuntimeException()

                    val result = api.setSupportInformation("test", ServerSupportInformation(supportPage = "test.page"))

                    result.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                }
            }
        }
    }
}