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

package de.akquinet.timref.registrationservice.persistance.messengerInstance

import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.HomeServer
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.OidcProvider
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.ProxyConfig
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.RawDataIDs
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.SingleSignOn
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.SynapseConfig
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.SynapseSpec
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.UserMappingConfig
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.UserMappingProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "messenger_instance")
class MessengerInstanceEntity(
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    var id: UUID? = null,

    @Version
    @Column(name = "version")
    var version: Long? = null,

    @Column(name = "server_name", unique = true)
    var serverName: String = "servername",

    @Column(name = "public_base_url", unique = true)
    var publicBaseUrl: String = "publicBaseUrl",

    @Column(name = "user_id")
    var userId: String? = null,

    @Column(name = "date_of_order")
    var dateOfOrder: LocalDate = LocalDate.now(),

    @Column(name = "end_of_life_date")
    var endDate: LocalDate = LocalDate.now(),

    @Column(name = "telematik_id")
    var telematikId: String? = null,

    @Column(name = "profession_id")
    var professionId: String? = null,

    @Column(name = "instance_Id")
    var instanceId: String = "instanceId",

    @Column(name = "active")
    var active: Boolean = true,

    @Column(name = "start_of_inactivity")
    var startOfInactivity: Long? = null
) {
    fun toSynapseSpec(clientSecret: String, issuer: String) =
        SynapseSpec(
            synapseConfig = SynapseConfig(
                HomeServer(serverName, publicBaseUrl), SingleSignOn(
                    listOf(
                        OidcProvider(
                            clientSecret = clientSecret,
                            issuer = issuer,
                            userMappingProvider = UserMappingProvider(config = UserMappingConfig())
                        )
                    )
                )
            ),
            proxyConfig = ProxyConfig(RawDataIDs(instanceId, telematikId!!, professionId!!)),
            logLevel = "INFO",
        )

    fun toMessengerInstance(): MessengerInstance {
        return MessengerInstance(
            id = this.id,
            version = this.version,
            serverName = this.serverName,
            publicBaseUrl = this.publicBaseUrl,
            userId = this.userId,
            dateOfOrder = this.dateOfOrder.toString(),
            endDate = this.endDate.toString(),
            telematikId = this.telematikId,
            professionId = this.professionId,
            instanceId = this.instanceId,
            active = this.active,
            startOfInactivity = this.startOfInactivity
        )
    }

    init {
        require(serverName.isNotBlank()) { "Servername darf nicht leer sein." }
        require(publicBaseUrl.isNotBlank()) { "URL darf nicht leer sein." }
    }
}

data class MessengerInstance(
    val id: UUID? = null,
    val version: Long? = null,
    val serverName: String,
    val publicBaseUrl: String,
    val userId: String? = null,
    val dateOfOrder: String? = null,
    val endDate: String? = null,
    val telematikId: String? = null,
    val professionId: String? = null,
    val instanceId: String? = null,
    val active: Boolean = true,
    val startOfInactivity: Long? = null
)
