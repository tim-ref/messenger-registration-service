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

import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.Contact
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ContactRole
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ServerSupportInformation
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "support_information")
class SupportInformationEntity (
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    var id: UUID? = null,

    @Column(name = "server_name", unique = true) var serverName: String,

    @Column(name = "user_id") var userId: String,

    @Column(name = "support_page") var supportPage: String?,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "support_information_id")
    @Column(name = "contacts")
    var contacts: List<SupportContactEntity> = mutableListOf()
){
    init {
        require(serverName.isNotBlank()) { "[serverName] darf nicht leer sein."}
        require(userId.isNotBlank()) { "[userId] darf nicht leer sein" }
        require(!supportPage.isNullOrBlank() || contacts.isNotEmpty()) { "[supportPage] und [contacts] dürfen nicht beide leer sein" }
    }

    fun toServerSupportInformation(): ServerSupportInformation =
        ServerSupportInformation(
            contacts = this.contacts.map {
                Contact(
                    role = contactRoleFromValue(it.role),
                    matrixId = it.matrixId,
                    emailAddress = it.emailAddress
                )
            },
            supportPage = this.supportPage
        )

    private fun contactRoleFromValue(value: String) : ContactRole = ContactRole.entries.first { it.value == value }
}
