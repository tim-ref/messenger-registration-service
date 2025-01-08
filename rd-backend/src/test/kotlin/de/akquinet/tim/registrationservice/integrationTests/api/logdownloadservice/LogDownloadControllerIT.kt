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

package de.akquinet.tim.registrationservice.integrationTests.api.logdownloadservice

import com.ninjasquad.springmockk.MockkBean
import de.akquinet.tim.registrationservice.api.logdownloadservice.LogDownloadResult
import de.akquinet.tim.registrationservice.api.logdownloadservice.LogDownloadService
import de.akquinet.tim.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(IntegrationTestConfiguration::class)
class LogDownloadControllerIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var logDownloadService: LogDownloadService

    val logDownloadUrl = { serverName: String, containerName: String ->
        "/logging/download/$serverName/$containerName"
    }

    init {
        this.describe("LogDownloadControllerTest") {
            val someContainerName = "synapse"
            val someServerName = "minji.kr.dreamcatcher.dev"

            it("answers OK with stream") {
                val someBytes = ByteArray(100_000_000) { (it % 2).toByte() }

                every {
                    logDownloadService.getLogs(someServerName, someContainerName)
                } returns LogDownloadResult.LogStream(someBytes.inputStream(), someBytes.size)

                val mvcResponse =
                    mockMvc
                        .get(logDownloadUrl(someServerName, someContainerName))
                        .andReturn()
                        .response

                mvcResponse.run {
                    status shouldBe HttpStatus.OK.value()
                    contentAsByteArray shouldBe someBytes
                    getHeader("Content-Length") shouldBe someBytes.size.toString()
                }
            }

            it("uses request parameters if they exist") {
                every { logDownloadService.getLogs(
                    serverName = eq(someServerName),
                    containerName = eq(someContainerName),
                    start = any(),
                    end = any()
                ) } returns LogDownloadResult.Unexpected("I am a mock")

                mockMvc.get(logDownloadUrl(someServerName, someContainerName))
                verify { logDownloadService.getLogs(someServerName, someContainerName, null, null) }

                mockMvc.get(logDownloadUrl(someServerName, someContainerName) + "?start=1")
                verify { logDownloadService.getLogs(someServerName, someContainerName, 1, null) }

                mockMvc.get(logDownloadUrl(someServerName, someContainerName) + "?end=2")
                verify { logDownloadService.getLogs(someServerName, someContainerName, null, 2) }

                mockMvc.get(logDownloadUrl(someServerName, someContainerName) + "?start=1&end=2")
                verify { logDownloadService.getLogs(someServerName, someContainerName, 1, 2) }
            }

            context("error cases") {
                it("answers UNAUTHORIZED if Unauthorized") {
                    every {
                        logDownloadService.getLogs(someServerName, someContainerName)
                    } returns LogDownloadResult.Unauthorized(someServerName)

                    val mvcResponse =
                        mockMvc
                            .get(logDownloadUrl(someServerName, someContainerName))
                            .andReturn()
                            .response

                    mvcResponse.run {
                        status shouldBe HttpStatus.UNAUTHORIZED.value()
                        contentAsString shouldContain "not an instance"
                        contentAsString shouldContain someServerName
                    }
                }

                it("answers BAD_REQUEST if InvalidInput") {
                    every {
                        logDownloadService.getLogs(someServerName, someContainerName)
                    } returns LogDownloadResult.InvalidInput("invalid input")

                    val mvcResponse =
                        mockMvc
                            .get(logDownloadUrl(someServerName, someContainerName))
                            .andReturn()
                            .response

                    mvcResponse.run {
                        status shouldBe HttpStatus.BAD_REQUEST.value()
                        contentAsString shouldContain "invalid input"
                    }
                }

                it("answers INTERNAL_SERVER_ERROR if Unexpected") {
                    every {
                        logDownloadService.getLogs(someServerName, someContainerName)
                    } returns LogDownloadResult.Unexpected("something bad")

                    val mvcResponse =
                        mockMvc
                            .get(logDownloadUrl(someServerName, someContainerName))
                            .andReturn()
                            .response

                    mvcResponse.run {
                        status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
                        contentAsString shouldContain "unexpected"
                        contentAsString shouldContain "something bad"
                    }
                }
            }
        }
    }
}
