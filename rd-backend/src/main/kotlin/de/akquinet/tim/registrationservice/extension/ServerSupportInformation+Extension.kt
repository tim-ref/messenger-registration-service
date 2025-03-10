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

package de.akquinet.tim.registrationservice.extension

import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ServerSupportInformation
import de.akquinet.tim.registrationservice.persistance.wellKnownSupport.model.SupportContactEntity
import de.akquinet.tim.registrationservice.persistance.wellKnownSupport.model.SupportInformationEntity

/* Parse a SupportInformation to an Entity
 *
 * parameters:
 *     [serverName] as a reference to the Homeserver instance
 *     [userId] as a reference to the Org-Admin (KeyCloak user)
 */
fun ServerSupportInformation.toEntity(serverName: String, userId: String): SupportInformationEntity =
    SupportInformationEntity(
        serverName = serverName,
        userId = userId,
        supportPage = supportPage,
        contacts = contacts?.map {
            SupportContactEntity(
                emailAddress = it.emailAddress,
                matrixId = it.matrixId,
                role = it.role.value
            )
        } ?: emptyList()
    )