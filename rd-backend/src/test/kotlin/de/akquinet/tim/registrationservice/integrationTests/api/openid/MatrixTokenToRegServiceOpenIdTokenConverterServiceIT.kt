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

package de.akquinet.tim.registrationservice.integrationTests.api.openid

import com.ninjasquad.springmockk.MockkBean
import de.akquinet.tim.registrationservice.persistance.orgAdmin.model.OrgAdminEntity
import de.akquinet.tim.registrationservice.security.signature.jose4extension.Brainpool
import de.akquinet.tim.registrationservice.service.openid.*
import de.akquinet.tim.registrationservice.service.orgadmin.OrgAdminManagementService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.security.Security
import java.util.*

private const val GOOD_TOKEN = "abc"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
class MatrixTokenToRegServiceOpenIdTokenConverterServiceIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var tokenConverterService: MatrixTokenToRegServiceOpenIdTokenConverterService

    @MockkBean(relaxed = true)
    lateinit var orgAdminService: OrgAdminManagementService

    @MockkBean(relaxed = true)
    lateinit var tokenValidator: MatrixTokenValidatorService

    init {
        this.describe("MatrixTokenToRegServiceOpenIdTokenConverterServiceTest") {
            // BC providers required for certificates and TLS cipher suites using brainpool curves
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            Brainpool.installExtension()

            it("should produce token for existing user") {
                val mxId = "@OrgAdmin:tim.akquinet.de"
                every {
                    tokenValidator.validateToken(mxId, GOOD_TOKEN, "tim.akquinet.de")
                } returns mxId

                every { orgAdminService.getByMxId(mxId) } returns OrgAdminEntity(
                    mxId = mxId,
                    id = UUID.randomUUID(),
                    telematikId = "akq-12345",
                    professionOid = "1.2.276.0.76.4.53",
                    serverName = "akq"
                )

                val result = tokenConverterService
                    .convertTokenForUser(mxId, GOOD_TOKEN)
                    .shouldBeInstanceOf<SuccessResult>()

                result.expiresIn shouldBe 3600
                result.accessToken shouldStartWith "ey"
            }

            it("should not produce token for nonexistent user") {
                val mxId = "@Nonexistent:tim.akquinet.de"
                every {
                    tokenValidator.validateToken(mxId, GOOD_TOKEN, "tim.akquinet.de")
                } returns mxId

                every { orgAdminService.getByMxId(mxId) } returns null

                val result = tokenConverterService
                    .convertTokenForUser(mxId, GOOD_TOKEN)
                    .shouldBeInstanceOf<ErrorResult>()

                result.errorType shouldBe TokenConverterResultErrorType.UNAUTHORIZED
            }

            it("should yield error on bad user id") {
                val mxId = "bad user id"

                val result = tokenConverterService
                    .convertTokenForUser(mxId, GOOD_TOKEN)
                    .shouldBeInstanceOf<ErrorResult>()

                result.errorType shouldBe TokenConverterResultErrorType.USER_INPUT
            }

            it("should yield error on invalid token") {
                val mxId = "@OrgAdmin:tim.akquinet.de"
                val badToken = "bad"
                every {
                    tokenValidator.validateToken(
                        mxId,
                        badToken,
                        "tim.akquinet.de"
                    )
                } answers  {throw InvalidTokenException("Unauthorized") }

                every { orgAdminService.getByMxId(mxId) } returns OrgAdminEntity(
                    mxId = mxId,
                    id = UUID.randomUUID(),
                    telematikId = "akq-12345",
                    professionOid = "1.2.276.0.76.4.53",
                    serverName = "akq"
                )

                val result = tokenConverterService
                    .convertTokenForUser(mxId, badToken)
                    .shouldBeInstanceOf<ErrorResult>()

                result.errorType shouldBe TokenConverterResultErrorType.UNAUTHORIZED
            }
        }
    }
}
