/*
 * Copyright (C) 2023 - 2024 akquinet GmbH
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

package de.akquinet.tim.registrationservice.integrationTests.api.messengerService

import com.ninjasquad.springmockk.SpykBean
import de.akquinet.tim.registrationservice.api.federation.FederationListServiceImpl
import de.akquinet.tim.registrationservice.persistance.federation.FederationListRepository
import de.akquinet.tim.registrationservice.security.signature.jose4extension.Brainpool
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.Enabled
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.security.Security

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests-with-vzd-test")
class FederationListIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @SpykBean
    lateinit var federationService: FederationListServiceImpl

    @Autowired
    lateinit var federationRepository: FederationListRepository

    init {
        // BC providers required for certificates and TLS cipher suites using brainpool curves and install custom algorithm
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        Security.insertProviderAt(BouncyCastleJsseProvider(), 1)
        Brainpool.installExtension()

        val isVzdSecretSet: (TestCase) -> Enabled = {
            if (System.getenv("TEST_VZD_CLIENT_SECRET")?.isNotEmpty() == true) {
                Enabled.enabled
            } else {
                Enabled.disabled("VZD Client Secret not set, skipping test")
            }
        }

        this.describe("Real Test VZD Test") {
            // BC providers required for certificates and TLS cipher suites using brainpool curves and install custom algorithm
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            Brainpool.installExtension()

            it("Fetch the real test federation list from VZD").config(enabledOrReasonIf = isVzdSecretSet) {
                federationService.getLatestFederationListFromVzd()

                verify(exactly = 1) { federationService.getVerifiedFederationListFromRemote(any()) }
                verify(exactly = 1) { federationService.saveFederationListToRepository(any()) }

                federationRepository.count() shouldBe 1
            }
        }
    }
}
