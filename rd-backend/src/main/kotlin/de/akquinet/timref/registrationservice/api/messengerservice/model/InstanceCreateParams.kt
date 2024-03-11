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

package de.akquinet.timref.registrationservice.api.messengerservice.model

import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import java.time.LocalDate

data class InstanceCreateParams(
    val userId: String,
    val dateOfOrder: LocalDate,
    val endDate: LocalDate,
    val currentInstanceCount: Int,
    val telematikId: String,
    val nextInstanceRandom: String,
    val nextInstanceName: String,
    val nextInstanceFQDN: String,
    val professionOid: String,
    val active: Boolean,
    val startOfInactivity: Long?
) {

    val instanceId: String
        get() = nextInstanceFQDN.replace(".", "")

    fun toMessengerInstanceEntity() = MessengerInstanceEntity(
        userId = userId,
        telematikId = telematikId,
        professionId = professionOid,
        instanceId = instanceId,
        dateOfOrder = dateOfOrder,
        endDate = endDate,
        serverName = nextInstanceFQDN,
        publicBaseUrl = nextInstanceFQDN,
        active = active,
        startOfInactivity = startOfInactivity
    )
}
