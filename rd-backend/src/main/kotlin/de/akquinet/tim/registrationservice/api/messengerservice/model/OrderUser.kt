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

package de.akquinet.tim.registrationservice.api.messengerservice.model

import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserAttributeKey.DATE_OF_ORDER
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserAttributeKey.ORDER_LENGTH
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserAttributeKey.PROFESSION_OID
import de.akquinet.tim.registrationservice.api.keycloak.KeycloakUserAttributeKey.TELEMATIK_ID
import de.akquinet.tim.registrationservice.util.ddMMyyyyDateTimeFormatter
import org.keycloak.representations.idm.UserRepresentation
import java.time.LocalDate

data class OrderUser(
    val username: String,
    val telematikId: String,
    val professionOid: String,
    val orderLength: String,
    val dateOfOrder: LocalDate,
)

fun UserRepresentation.toOrderUser(): OrderUser = OrderUser(
    username = this.username.trim(),
    telematikId = this.firstAttribute(TELEMATIK_ID.value).trim(),
    professionOid = this.firstAttribute(PROFESSION_OID.value).trim(),
    orderLength = this.firstAttribute(ORDER_LENGTH.value).trim(),
    dateOfOrder = LocalDate.parse(
        this.firstAttribute(DATE_OF_ORDER.value).trim(), ddMMyyyyDateTimeFormatter
    )
)
