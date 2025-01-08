/*
 * Copyright (C) 2023-2024 akquinet GmbH
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

package de.akquinet.tim.registrationservice.api.federation.model

import com.google.gson.Gson
import de.akquinet.tim.registrationservice.openapi.model.federation.Domain
import de.akquinet.tim.registrationservice.security.Base64String
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

data class FederationListBase64Header(
    val alg: String,
    val x5c: List<Base64String>
)

data class FederationListHeader(
    val alg: String,
    val x5c: List<String>
) {
    internal fun toFederationListBase64Header(): FederationListBase64Header {
        return FederationListBase64Header(alg = this.alg, x5c = this.x5c.map { Base64String(it.replace("\n", "").trimEnd()) })
    }
}

data class AddDomainResponse(
    val httpStatus: HttpStatus,
    val domain: Domain? = null,
    val errorMessage: String? = null
) {
    fun toResponseEntity(gson: Gson): ResponseEntity<String> =
        domain?.let {
            ResponseEntity.status(httpStatus).body(gson.toJson(it))
        } ?: ResponseEntity.status(httpStatus).body(errorMessage)
}

data class DeleteDomainResponse(
    val httpStatus: HttpStatus,
    val errorMessage: String? = null
) {
    fun toResponseEntity(): ResponseEntity<String> =
        errorMessage?.let {
            ResponseEntity.status(httpStatus).body(errorMessage)
        } ?: ResponseEntity.status(httpStatus).body("")
}