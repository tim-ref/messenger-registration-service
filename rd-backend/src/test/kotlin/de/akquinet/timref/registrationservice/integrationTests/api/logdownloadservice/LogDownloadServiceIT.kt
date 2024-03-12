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

package de.akquinet.timref.registrationservice.integrationTests.api.logdownloadservice

import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returns
import de.akquinet.timref.registrationservice.api.logdownloadservice.LogDownloadLokiConfig
import de.akquinet.timref.registrationservice.api.logdownloadservice.LogDownloadResult
import de.akquinet.timref.registrationservice.api.logdownloadservice.LogDownloadServiceImpl
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.timref.registrationservice.rawdata.RawDataService
import de.akquinet.timref.registrationservice.util.UserService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.LocalDate

class LogDownloadServiceIT : DescribeSpec() {

    private var userService: UserService = mockk()

    private var messengerInstanceRepository: MessengerInstanceRepository = mockk()

    private val rawDataService: RawDataService = mockk(relaxed = true)
    private val lokiWiremockPort = 7777
    private val lokiTestUrl = "/loki"
    private val testLokiConfig = LogDownloadLokiConfig(
        protocol = "http",
        host = "localhost",
        path = lokiTestUrl,
        port =  lokiWiremockPort
    )

    private val logDownloadService = LogDownloadServiceImpl(
        LoggerFactory.getLogger(javaClass.name),
        testLokiConfig,
        userService,
        messengerInstanceRepository,
        rawDataService
    )
    init {
        val lokiWiremock = WireMockServer(lokiWiremockPort)

        val someContainerName = "synapse"
        val someServerName = "minji.kr.dreamcatcher.dev"
        val someUser = "JiU"

        beforeEach {
            every { userService.getUserIdFromContext() } returns someUser
            every {
                messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(someServerName, someUser)
            } returns MessengerInstanceEntity(
                dateOfOrder = LocalDate.now(),
                endDate = LocalDate.now().plusMonths(1),
                userId = someUser,
                professionId = "someProfessionId",
                publicBaseUrl = someServerName,
                instanceId = someServerName.replace(".", ""),
                serverName = someServerName,
                telematikId = "someTelematikId"
            )

            lokiWiremock.start()
        }

        afterEach {
            lokiWiremock.resetAll()
            lokiWiremock.stop()
        }

        describe("LogDownloadServiceTest") {
            it("works with 100MB without going OOM") {
                val someBytes = ByteArray(100_000_000) { (it % 2).toByte() }

                lokiWiremock.get {
                    url contains lokiTestUrl
                    url contains someContainerName
                } returns {
                    statusCode = HttpStatus.OK.value()
                    body = String(someBytes)
                }

                val result = logDownloadService.getLogs(someServerName, someContainerName)
                result
                    .shouldBeInstanceOf<LogDownloadResult.LogStream>()
                    .inputStream
                    .use {
                        it.readBytes() shouldBe someBytes
                    }
            }

            context("error cases") {
                it("answers Unauthorized if MessengerInstanceEntity is null") {
                    every {
                        messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(someServerName, someUser)
                    } returns null

                    val result = logDownloadService.getLogs(someServerName, someContainerName)
                    result.shouldBeInstanceOf<LogDownloadResult.Unauthorized>()
                }

                it("answers InvalidInput if start >= end") {
                    val now = Instant.now().epochSecond

                    val result = logDownloadService.getLogs(
                        serverName = someServerName,
                        containerName = someContainerName,
                        start = now,
                        end = now - 60
                    )
                    result.shouldBeInstanceOf<LogDownloadResult.InvalidInput>()
                }

                it("answers InvalidInput if end is in the future") {
                    val now = Instant.now().epochSecond

                    val result = logDownloadService.getLogs(
                        serverName = someServerName,
                        containerName = someContainerName,
                        end = now + 60
                    )
                    result.shouldBeInstanceOf<LogDownloadResult.InvalidInput>()
                }

                it("answers Unexpected if Loki response is not OK") {
                    lokiWiremock.get {
                        url contains lokiTestUrl
                        url contains someContainerName
                    } returns {
                        statusCode = HttpStatus.NOT_FOUND.value()
                        body = "NOT SUCCESSFULL"
                    }

                    val result = logDownloadService.getLogs(someServerName, someContainerName)
                    result
                        .shouldBeInstanceOf<LogDownloadResult.Unexpected>()
                        .hint
                        .shouldContain("Loki response was unsuccessful")
                }

                it("answers Unexpected if Loki response body is null") {
                    lokiWiremock.get {
                        url contains lokiTestUrl
                        url contains someContainerName
                    } returns {
                        statusCode = HttpStatus.OK.value()
                    }

                    val result = logDownloadService.getLogs(someServerName, someContainerName)
                    result
                        .shouldBeInstanceOf<LogDownloadResult.Unexpected>()
                        .hint
                        .shouldBe("Loki response body was null")
                }
            }
        }
    }
}
