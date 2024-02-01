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

package de.akquinet.timref.registrationservice.persistance.federation.model

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "domain")
class DomainEntity(
    @Column(name = "domain") var domain: String = "domain",

    @Column(name = "is_insurance") var isInsurance: Boolean = false,

    @Column(name = "telematik_id") var telematikID: String = "telematikId",

    ) {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    var id: UUID? = null

    init {
        require(domain.isNotBlank()) { "Domain darf nicht leer sein." }
        require(telematikID.isNotBlank()) { "TelematikID darf nicht leer sein." }
    }
}
