/*
 * Copyright (C) 2024 akquinet GmbH
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

package de.akquinet.tim.registrationservice.unitTests.service.messengerservice

import de.akquinet.tim.registrationservice.api.messengerservice.MessengerInstanceCheckService
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.LocalDate

class MessengerInstanceCheckServiceTest : DescribeSpec() {

    init {
        describe("MessengerInstanceCheckServiceTest") {

            it("calculates correct EOL date") {

                val sut = MessengerInstanceCheckService(
                    logger = mockk {},
                    messengerInstanceRepository = mockk {},
                    messengerInstanceDeleteService = mockk {},
                    meterRegistry = mockk {}
                )
                val exampleInstance = MessengerInstanceEntity(
                    null,
                    0,
                    "exampleName",
                    "exampleURL",
                    "",
                    LocalDate.now(),
                    LocalDate.of(2024, 10, 10),
                    "",
                    "",
                    "1",
                    true,
                    null
                )
                val testCases = listOf(
                    // Over 1 year before EOL
                    Pair(LocalDate.of(2023, 10, 10), 366),   // 366 days because 2024 is a leap year

                    Pair(LocalDate.of(2024, 9, 1), 39),      // 39 days before EOL
                    Pair(LocalDate.of(2024, 9, 10), 30),     // 30 days before EOL
                    // 1 day before EOL
                    Pair(LocalDate.of(2024, 10, 9), 1),      // 1 day before EOL
                    // On EOL date
                    Pair(LocalDate.of(2024, 10, 10), 0),     // On EOL date
                    // After EOL date
                    Pair(LocalDate.of(2024, 10, 11), -1),    // 1 day after EOL
                    // Over 1 year after EOL
                    Pair(LocalDate.of(2025, 10, 10), -365),  // 365 days after EOL
                )

                testCases.forEach { (currentDate, expectedDaysToEOL) ->
                    val actual = sut.calcDaysToEOLOfInstance(exampleInstance, currentDate)
                    actual shouldBe expectedDaysToEOL
                }
            }

        }
    }
}
