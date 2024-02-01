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

package de.akquinet.timref.registrationservice.integrationTests.api.openid

import com.ninjasquad.springmockk.MockkBean
import de.akquinet.timref.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import de.akquinet.timref.registrationservice.service.openid.ErrorResult
import de.akquinet.timref.registrationservice.service.openid.MatrixTokenToRegServiceOpenIdTokenConverterService
import de.akquinet.timref.registrationservice.service.openid.SuccessResult
import de.akquinet.timref.registrationservice.service.openid.TokenConverterResultErrorType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(IntegrationTestConfiguration::class)
class RegServiceOpenIdTokenControllerIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var tokenConverterService: MatrixTokenToRegServiceOpenIdTokenConverterService

    init {
        this.describe("RegServiceOpenIdTokenControllerTest") {
            it("should produce good result") {
                val mxId = "@OrgAdmin:tim.akquinet.de"
                val matrixToken = "abc"
                every {
                    tokenConverterService.convertTokenForUser(mxId, matrixToken)
                } returns SuccessResult("result", 1234L)


                mockMvc.post("/regservice/openid/user/$mxId/requesttoken") {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                    content = "request_token=$matrixToken"
                }.andDo {
                    print()
                }.andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON_VALUE) }
                    content { string("{\"access_token\":\"result\",\"expires_in\":1234,\"token_type\":\"Bearer\"}") }
                }
            }

            it("should handle unauthorized user") {
                every{
                    tokenConverterService.convertTokenForUser("@OrgAdmin:tim.akquinet.de", "abc")
                } returns ErrorResult(TokenConverterResultErrorType.UNAUTHORIZED)

                mockMvc.post("/regservice/openid/user/@OrgAdmin:tim.akquinet.de/requesttoken") {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                    content = "request_token=abc"
                }.andDo {
                    print()
                }.andExpect {
                    status { isEqualTo(400) }
                    content { contentType(MediaType.APPLICATION_JSON_VALUE) }
                    content { string("{\"error\":\"unauthorized\"}") }
                }
            }

            it("should yield error on bad user id") {
                every {
                    tokenConverterService.convertTokenForUser("@OrgAdmin:tim.akquinet.de", "abc")
                } returns ErrorResult(TokenConverterResultErrorType.USER_INPUT, "reason")

                mockMvc.post("/regservice/openid/user/@OrgAdmin:tim.akquinet.de/requesttoken") {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                    content = "request_token=abc"
                }.andDo {
                    print()
                }.andExpect {
                    status { isEqualTo(400) }
                    content { contentType(MediaType.APPLICATION_JSON_VALUE) }
                    content { string("{\"error\":\"reason\"}") }
                }
            }
        }
    }
}
