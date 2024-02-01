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

package de.akquinet.timref.registrationservice.integrationTests

import de.akquinet.timref.registrationservice.integrationTests.configuration.IntegrationTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(IntegrationTestConfiguration::class)
class IntegrationTests : DescribeSpec() {

    @Autowired
    lateinit var embeddedDatasource: DataSource

    @PostConstruct
    fun migrateWithFlyway() {
        val flyway = Flyway.configure().dataSource(embeddedDatasource).locations("db/migration", "db/testdata-dev")
            .cleanOnValidationError(true).load()
        flyway.migrate()
    }

    init {
        this.describe("IntegrationTests") {
            it("loads context") {
                1 shouldBe 1
            }
        }
    }
}
