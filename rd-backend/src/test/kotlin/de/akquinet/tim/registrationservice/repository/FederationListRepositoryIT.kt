package de.akquinet.tim.registrationservice.repository

/*
 * Copyright (C) 2024 - 2025 akquinet GmbH
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

import de.akquinet.tim.registrationservice.persistance.federation.FederationListRepository
import de.akquinet.tim.registrationservice.persistance.federation.model.FederationListEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests-without-testdata")
class FederationListRepositoryIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var federationListRepository: FederationListRepository

    init {
        this.describe("Federation Repository Test") {

            it("should return max version of federation lists") {

                val dummyOldFederationList = FederationListEntity(
                    version = 10L,
                    domainList = emptyList()
                )

                val dummyNewFederationList = FederationListEntity(
                    version = 11L,
                    domainList = emptyList()
                )

                val dummyFederationEntityList = listOf(dummyOldFederationList, dummyNewFederationList)

                federationListRepository.saveAll(dummyFederationEntityList)

                val actualVersion =
                    federationListRepository.findMaxVersion()

                actualVersion shouldBe 11L
            }

        }
    }
}
