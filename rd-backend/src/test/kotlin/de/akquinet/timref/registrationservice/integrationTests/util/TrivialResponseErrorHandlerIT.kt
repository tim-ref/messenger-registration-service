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

package de.akquinet.timref.registrationservice.integrationTests.util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returns
import de.akquinet.timref.registrationservice.util.TrivialResponseErrorHandler
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException.InternalServerError

class TrivialResponseErrorHandlerIT : DescribeSpec() {
    init {
        describe("TrivialResponseErrorHandlerTest") {

            val wiremockPort = 7777
            val wiremockUrl = { path: String -> "http://localhost:$wiremockPort/$path" }

            val wiremock = WireMockConfiguration
                .options()
                .port(wiremockPort)
                .notifier(ConsoleNotifier(true))
                .let { WireMockServer(it) }

            beforeEach {
                wiremock.start()
            }

            afterEach {
                wiremock.resetAll()
                wiremock.stop()
            }

            context("RestTemplate with custom error handler") {
                val restTemplate = RestTemplateBuilder()
                    .errorHandler(TrivialResponseErrorHandler())
                    .build()

                it("throws nothing if code is 4xx") {
                    wiremock.get {
                        url equalTo "/test400"
                    } returns {
                        statusCode = HttpStatus.BAD_REQUEST.value()
                        body = "Hi!"
                    }

                    val response = restTemplate.exchange(
                        wiremockUrl("test400"),
                        HttpMethod.GET,
                        null,
                        String::class.java
                    )

                    response.statusCode shouldBe HttpStatus.BAD_REQUEST
                    response.body shouldBe "Hi!"
                }

                it("throws nothing if code is 5xx") {
                    wiremock.get {
                        url equalTo "/test500"
                    } returns {
                        statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value()
                        body = "Hi!"
                    }

                    val response = restTemplate.exchange(
                        wiremockUrl("test500"),
                        HttpMethod.GET,
                        null,
                        String::class.java
                    )

                    response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                    response.body shouldBe "Hi!"
                }
            }

            context("normal spring behaviour for comparison") {
                // do not use custom error handler
                val restTemplate = RestTemplateBuilder().build()

                it("throws exception if code is 4xx") {
                    wiremock.get {
                        url equalTo "/test400"
                    } returns {
                        statusCode = HttpStatus.BAD_REQUEST.value()
                        body = "Hi!"
                    }

                    val exception = shouldThrow<HttpClientErrorException> {
                        restTemplate.exchange(
                            wiremockUrl("test400"),
                            HttpMethod.GET,
                            null,
                            String::class.java
                        )
                    }

                    exception.statusCode shouldBe HttpStatus.BAD_REQUEST
                }

                it("throws exception if code is 5xx") {
                    wiremock.get {
                        url equalTo "/test500"
                    } returns {
                        statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value()
                        body = "Hi!"
                    }

                    val exception = shouldThrow<InternalServerError> {
                        restTemplate.exchange(
                            wiremockUrl("test500"),
                            HttpMethod.GET,
                            null,
                            String::class.java
                        )
                    }

                    exception.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                }
            }
        }
    }
}
