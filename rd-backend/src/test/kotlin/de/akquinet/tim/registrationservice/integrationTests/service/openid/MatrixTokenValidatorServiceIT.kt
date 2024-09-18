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

package de.akquinet.tim.registrationservice.integrationTests.service.openid

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.marcinziolo.kotlin.wiremock.*
import de.akquinet.tim.registrationservice.config.MatrixConfig
import de.akquinet.tim.registrationservice.service.openid.InvalidTokenException
import de.akquinet.tim.registrationservice.service.openid.MatrixTokenValidatorService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk

private const val USERINFO_URL = "/_matrix/federation/v1/openid/userinfo"

class MatrixTokenValidatorServiceIT : DescribeSpec() {

    private val wiremock = WireMockServer(WireMockConfiguration.options().port(1234).notifier(ConsoleNotifier(true)))

    private val mockName = "localhost"

    private val config
        get() = MatrixConfig("http", wiremock.port(), "http", wiremock.port())

    private val mxId
        get() = "@admin:${mockName}"

    init {
        beforeEach {
            wiremock.start()
        }

        afterEach {
            wiremock.resetAll()
            wiremock.stop()
        }

        describe("MatrixTokenValidatorServiceTest") {
            it("can exchange and validate token") {
                val token = "abc"

                wiremock.get {
                    url contains USERINFO_URL
                    queryParams contains "access_token" equalTo token
                } returnsJson {
                    body = """{"sub": "$mxId"}"""
                }

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})

                val result = subjectUnderTest.validateToken(mxId, token, mockName)

                result shouldBe mxId
            }

            it("throws InvalidTokenException if mxId from matrix server doesn't match the supplied one") {
                val token = "abc"
                val anotherMxId = "@anotherAdmin:anotherServer"

                wiremock.get {
                    url contains USERINFO_URL
                    queryParams contains "access_token" equalTo token
                } returnsJson {
                    body = """{"sub": "$anotherMxId"}"""
                }

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})
                shouldThrow<InvalidTokenException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)
                }
            }

            it("throws InvalidTokenException if token is unauthorized") {
                val token = "abc"

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})

                shouldThrow<InvalidTokenException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)
                }
            }

            it("throws InvalidTokenException if token is not valid at all") {
                val token = "abc"

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})

                shouldThrow<InvalidTokenException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)
                }
            }

            it("throws InvalidTokenException if matrix does not give userinfo on its own token") {
                val token = "abc"

                wiremock.get {
                    url contains USERINFO_URL
                    queryParams contains "access_token" equalTo token
                } returnsJson {
                    body = """{"error": "too bad"}"""
                    statusCode = 401
                }

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})
                shouldThrow<InvalidTokenException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)
                }
            }

            it("throws RuntimeException if matrix userinfo returns unexpected stuff") {
                val token = "abc"

                wiremock.get {
                    url contains USERINFO_URL
                    queryParams contains "access_token" equalTo token
                } returnsJson {
                    body = """{"youDidNot": "see this one coming"}"""
                }

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})
                shouldThrow<RuntimeException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)    // code in here that you expect to throw an IllegalAccessException
                }
            }

            it("throws RuntimeException if matrix userinfo returns no body") {
                val token = "abc"

                wiremock.get {
                    url contains USERINFO_URL
                    queryParams contains "access_token" equalTo token
                } returns {
                    statusCode = 200
                }

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})
                shouldThrow<RuntimeException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)
                }
            }

            it("throws RuntimeException if matrix request_token returns unexpected stuff") {
                val token = "abc"

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})
                shouldThrow<RuntimeException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)
                }
            }

            it("throws RuntimeException if matrix request_token returns no body") {
                val token = "abc"

                val subjectUnderTest = MatrixTokenValidatorService(spyk {}, config, spyk {})
                shouldThrow<RuntimeException> {
                    subjectUnderTest.validateToken(mxId, token, mockName)
                }
            }
        }
    }
}
