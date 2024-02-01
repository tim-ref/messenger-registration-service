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

package de.akquinet.timref.registrationservice.integrationTests.api.messengerService

import com.ninjasquad.springmockk.SpykBean
import de.akquinet.timref.registrationservice.api.federation.FederationServiceImpl
import de.akquinet.timref.registrationservice.persistance.federation.FederationRepository
import de.akquinet.timref.registrationservice.security.signature.jose4extension.Brainpool
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.Enabled
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.annotation.PostConstruct
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.security.Security
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests-with-vzd-test")
class FederationListIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @SpykBean
    lateinit var federationService: FederationServiceImpl

    @Autowired
    lateinit var federationRepository: FederationRepository

    @Autowired
    lateinit var embeddedDatasource: DataSource

    @PostConstruct
    fun migrateWithFlyway() {
        val flyway = Flyway.configure().dataSource(embeddedDatasource).locations("db/migration")
            .cleanOnValidationError(true).load()
        flyway.migrate()
    }

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
                federationService.getFederationListResponse()

                verify(exactly = 1) { federationService.getVerifiedFederationListFromRemote(any()) }
                verify(exactly = 1) { federationService.saveFederationListToRepository(any()) }

                federationRepository.count() shouldBe 1
            }
        }
    }
}
