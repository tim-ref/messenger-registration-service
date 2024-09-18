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

package de.akquinet.tim.registrationservice.unitTests.api.federation

import de.akquinet.tim.registrationservice.api.federation.AuthenticationTokenException
import de.akquinet.tim.registrationservice.api.federation.FederationServiceImpl
import de.akquinet.tim.registrationservice.api.federation.Token
import de.akquinet.tim.registrationservice.api.federation.VzdConnectionException
import de.akquinet.tim.registrationservice.api.federation.model.Domain
import de.akquinet.tim.registrationservice.api.federation.model.FederationList
import de.akquinet.tim.registrationservice.config.VZDConfig
import de.akquinet.tim.registrationservice.security.signature.JwsVerificationResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection

class FederationServiceTest : DescribeSpec({
    describe("FederationServiceTest") {
        val federationListDummy = FederationList(
            version = 32,
            domainList = listOf(Domain(domain = "test1.de", isInsurance = false, telematikID = "iaf983asdf"))
        )

        fun connectionWithCode(code: Int): HttpURLConnection = mockk<HttpURLConnection>() {
            every { responseCode } returns  code
        }

        it("behaves correctly if VZD returns status code 200, depending on signature") {
            /**
             * - returns correct FederationListResponse depending on signature validity
             * - saves federation list to repository if the signature is valid
             */
            class TestFederationServiceImplValid : FederationServiceImpl(
                federationRepository = mockk(relaxed = true),
                vzdConfig = VZDConfig(
                    serviceUrl = "url",
                    tokenUrl = "url",
                    tokenPath = "path",
                    authenticationPath = "authPath",
                    healthPath = "healthPath",
                    federationListPath = "fedPath",
                    federationCheckPath = "checkPath",
                    userWhereIsPath = "wherePath",
                    checkRevocationStatus = false,
                    addDomainPath = "domainPath",
                    deleteDomainPath = "deletePath",
                    clientId = "clientId",
                    clientSecret = "clientSecret",
                    trustStorePath = "path/to/secret",
                    trustStorePassword = "trustStorePassword"
                ),
                signatureService = mockk(),
                rawdataService = mockk(),
                logger = LoggerFactory.getLogger(javaClass.name)
            ) {
                var saveFederationListCalls = 0

                override fun getVerifiedFederationListFromRemote(response: HttpURLConnection): JwsVerificationResult =
                        JwsVerificationResult.Valid(federationListDummy)

                override fun saveFederationListToRepository(federationList: FederationList) {
                    saveFederationListCalls++
                }

                override fun connectToVzd(uri: String, queryParameters: Map<String, Any>?) = connectionWithCode(200)
            }

            class TestFederationServiceImplInvalid: TestFederationServiceImplValid() {
                override fun getVerifiedFederationListFromRemote(response: HttpURLConnection): JwsVerificationResult =
                        JwsVerificationResult.Invalid
            }

            run {
                val federationService = TestFederationServiceImplValid()
                federationService.getFederationListResponse()

                federationService.saveFederationListCalls shouldBe 1
            }

            run {
                val federationService = TestFederationServiceImplInvalid()
                federationService.getFederationListResponse()

                federationService.saveFederationListCalls shouldBe 0
            }
        }
    }
    describe("Exception handling in Federation Service") {
        val dummyToken = Token("", "", 0L)
        val testFederationService = FederationServiceImpl(
            logger = LoggerFactory.getLogger(FederationServiceImpl::class.java),
            federationRepository = mockk(),
            vzdConfig = VZDConfig(
                serviceUrl = "https://fhir-directory-test.vzd.ti-dienste.de",
                tokenUrl = "https://auth-test.vzd.ti-dienste.de:9443",
                tokenPath = "/auth/realms/TI-Provider/protocol/openid-connect/token",
                authenticationPath = "authPath",
                healthPath = "healthPath",
                federationListPath = "fedPath",
                federationCheckPath = "checkPath",
                userWhereIsPath = "wherePath",
                checkRevocationStatus = false,
                addDomainPath = "domainPath",
                deleteDomainPath = "deletePath",
                clientId = "clientId",
                clientSecret = "clientSecret",
                trustStorePath = "path/to/secret",
                trustStorePassword = "trustStorePassword"
            ),
            signatureService = mockk(),
            rawdataService = mockk(),
        )

        it("should throw VzdConnectionException exception on getVZDBearerToken failure") {

            shouldThrow<VzdConnectionException> { testFederationService.getVZDBearerToken() }
        }

        it("should throw AuthenticationTokenException exception on authenticateAtVZD failure") {

            shouldThrow<AuthenticationTokenException> { testFederationService.authenticateAtVZD(dummyToken) }

        }
    }
})
