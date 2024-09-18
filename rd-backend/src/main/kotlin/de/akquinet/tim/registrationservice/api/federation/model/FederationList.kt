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

package de.akquinet.tim.registrationservice.api.federation.model

import com.google.gson.Gson
import de.akquinet.tim.registrationservice.persistance.federation.model.DomainEntity
import de.akquinet.tim.registrationservice.persistance.federation.model.FederationListEntity
import de.akquinet.tim.registrationservice.security.Base64String
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

data class FederationListResponse(
    val httpStatus: HttpStatus,
    val federationList: FederationList? = null,
    val errorMessage: String? = null
) {
    fun toResponseEntity(gson: Gson): ResponseEntity<String> =
        federationList?.let {
            ResponseEntity.status(httpStatus).body(gson.toJson(it))
        } ?: ResponseEntity.status(httpStatus).body(errorMessage)
}

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

@Schema(name = "FederationList", description = "The federation list for the Messengerproxy")
data class FederationList(
    @field:Schema(description = "The version of the federation list", example = "") val version: Long,
    @field:Schema(description = "The list of hashed TI-Messenger domain names", example = "")
    val domainList: List<Domain>
) {
    internal fun toEntity(): FederationListEntity {
        return FederationListEntity(
            version = version,
            domainList = domainList.map { DomainEntity(domain = it.domain, isInsurance = it.isInsurance, telematikID = it.telematikID!!) })
    }
}

@Schema(name = "Hashed TI-Messenger domain name")
data class Domain(
    @field:Schema(description = "hashed TI-Messenger domain name", example = "") val domain: String,
    @field:Schema(example = "Indicates if it is a domain of an health insurance for insured persons")
    val isInsurance: Boolean,
    @field:Schema(description = "Telematik ID", example = "") val telematikID: String? = null
)
