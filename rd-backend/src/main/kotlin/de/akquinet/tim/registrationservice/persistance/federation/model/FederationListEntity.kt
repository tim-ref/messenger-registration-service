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

package de.akquinet.tim.registrationservice.persistance.federation.model

import de.akquinet.tim.registrationservice.api.federation.model.Domain
import de.akquinet.tim.registrationservice.api.federation.model.FederationList
import jakarta.persistence.*
import java.util.*


@Entity
@Table(name = "federation_list")
class FederationListEntity(
    @Version
    @Column(name = "version")
    var version: Long = 1,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "federation_list_id")
    var domainList: List<DomainEntity> = mutableListOf()
) {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    var id: UUID? = null

    internal fun toModel(): FederationList {
        return FederationList(version = version, domainList = domainList.map { Domain(it.domain, isInsurance = it.isInsurance, telematikID = it.telematikID ) })
    }
}
