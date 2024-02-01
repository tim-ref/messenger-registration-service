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

package de.akquinet.timref.registrationservice.unitTests

import de.akquinet.timref.registrationservice.security.decodeToString
import de.akquinet.timref.registrationservice.security.encodeToBase64Url
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class Base64Test : DescribeSpec({
    describe("Base64Test") {
        it("encodes and decodes Base64") {
            val plainText = "What is Love?"

            val encoded = plainText.encodeToBase64Url()
            encoded.value shouldBe "V2hhdCBpcyBMb3ZlPw=="

            val decoded = encoded.decodeToString()
            decoded shouldBe plainText
        }
    }
})