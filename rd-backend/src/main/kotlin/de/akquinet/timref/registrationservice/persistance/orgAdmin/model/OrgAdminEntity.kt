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

package de.akquinet.timref.registrationservice.persistance.orgAdmin.model

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "org_admin")
class OrgAdminEntity(
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    var id: UUID? = null,

    @Column(name = "telematik_id") var telematikId: String,

    @Column(name = "mx_id", unique = true) var mxId: String,

    @Column(name = "profession_oid", unique = true) var professionOid: String,

    @Column(name = "server_name", unique = true) var serverName: String,
)

fun extractSynapseServerNameFromMxId(userId: String): String? = if (userId.contains(":")) {
    userId.split(":").last()
} else {
    null
}
