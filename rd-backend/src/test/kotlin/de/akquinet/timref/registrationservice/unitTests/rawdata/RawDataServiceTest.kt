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

package de.akquinet.timref.registrationservice.unitTests.rawdata

import de.akquinet.timref.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import de.akquinet.timref.registrationservice.rawdata.RawDataServiceImpl
import de.akquinet.timref.registrationservice.rawdata.model.Operation
import de.akquinet.timref.registrationservice.rawdata.model.RawData
import de.akquinet.timref.registrationservice.rawdata.model.RawDataMetaData
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(IntegrationTestConfiguration::class)
class RawDataServiceTest : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var rawDataService: RawDataServiceImpl

    init {

        beforeEach {
            val javaWebToken = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "user")
                .claim("telematik_id", "telematikId")
                .claim("profession_oid", "professionOid")
                .build()
            val authentication: Authentication = mockk()
            val securityContext: SecurityContext = mockk()
            every { securityContext.authentication } returns authentication
            every { securityContext.authentication.principal } returns javaWebToken
            SecurityContextHolder.setContext(securityContext)
        }

        this.describe("Raw data service") {
            val rawDataDummy = RawData(
                `Inst-ID` = "user",
                `UA-A` = "n/a",
                `UA-OS` = "n/a",
                `UA-P` = "n/a",
                `UA-OS-VERSION` = "n/a",
                `UA-PTV` = "n/a",
                `UA-PV` = "n/a",
                `UA-cid` = "n/a",
                `M-Dom` = "",
                sizeIn = 13,
                sizeOut = 56,
                tID = "telematikId",
                profOID = "professionOid",
                Res = HttpStatus.OK.value().toString()
            )

            val dummyTimestamp = Instant.now()

            val rawDataMetaData = RawDataMetaData(
                start = dummyTimestamp,
                durationInMs = 100,
                operation = Operation.RS_CREATE_MESSENGER_SERVICE,
                status = HttpStatus.OK.value().toString(),
                message = rawDataDummy
            )

            val request: HttpServletRequest = mockk {
                every { getHeader("Content-Length") } returns  "13"
            }

            val responseString = "Erstelle Instanz mit Servername :hallo und URL: hallo.de"

            it("collects raw data and sends it") {

                rawDataService
                    .collectAndSendRawData(
                        request.getHeader("Content-Length")?.toIntOrNull() ?: 0,
                        responseString.length,
                        HttpStatus.OK,
                        Duration.parse("100ms"),
                        Operation.RS_CREATE_MESSENGER_SERVICE,
                        ""
                    )
                    .copy(start = dummyTimestamp)
                    .shouldBe(rawDataMetaData)
            }
        }
    }

}
