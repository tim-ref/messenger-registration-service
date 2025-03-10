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

package de.akquinet.tim.registrationservice.api.wellknownsupport

import de.akquinet.tim.registrationservice.extension.toEntity
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ServerSupportInformation
import de.akquinet.tim.registrationservice.persistance.wellKnownSupport.SupportInformationRepository
import de.akquinet.tim.registrationservice.util.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WellKnownSupportService @Autowired constructor(
    private val supportInfoRepository: SupportInformationRepository,
    private val userService: UserService
) {
    fun getSupportInformationForServerName(serverName: String): ServerSupportInformation? =
        supportInfoRepository.findByServerName(serverName)?.toServerSupportInformation()

    fun setSupportInformationForServerName(
        serverName: String,
        supportInformation: ServerSupportInformation,
        userId: String? = null
    ) {
        val entity = supportInformation.toEntity(
            serverName = serverName,
            userId = userId ?: userService.getUserIdFromContext()
        )
        val availableEntity = supportInfoRepository.findByServerName(serverName)

        if(availableEntity != null && availableEntity.userId != entity.userId) {
            throw NotAllowedException("Sie sind nicht berechtigt die Information für $serverName zu ändern")
        }
        entity.id = availableEntity?.id

        supportInfoRepository.save(entity)
    }
}
