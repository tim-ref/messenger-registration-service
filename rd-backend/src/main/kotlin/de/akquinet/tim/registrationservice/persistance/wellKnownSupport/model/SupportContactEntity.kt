/*
 * Copyright (C) 2025 akquinet GmbH
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

package de.akquinet.tim.registrationservice.persistance.wellKnownSupport.model

import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ContactRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "support_contact")
class SupportContactEntity (
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    var id: UUID? = null,

    @Column(name = "email_address")
    var emailAddress: String?,

    @Column(name = "matrix_id")
    var matrixId: String?,

    @Column(name = "role", nullable = false)
    var role: String
) {
    init {
        require(ContactRole.entries.any { it.value == role }) {
            "[role] muss einem der folgenden Werte entsprechen: [${ContactRole.entries.map { it.value }.joinToString(separator = ", ")}]"
        }
        require(!emailAddress.isNullOrBlank() || !matrixId.isNullOrBlank()) { "[emailAddress] und [matrixId] dürfen nicht beide leer sein." }
    }
}