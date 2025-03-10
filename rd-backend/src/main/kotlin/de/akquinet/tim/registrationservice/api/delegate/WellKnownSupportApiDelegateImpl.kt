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

package de.akquinet.tim.registrationservice.api.delegate

import de.akquinet.tim.registrationservice.api.wellknownsupport.NotAllowedException
import de.akquinet.tim.registrationservice.api.wellknownsupport.WellKnownSupportService
import de.akquinet.tim.registrationservice.openapi.api.wellknownsupport.server.WellKnownSupportApiDelegate
import de.akquinet.tim.registrationservice.openapi.model.wellknownsupport.ServerSupportInformation
import de.akquinet.tim.registrationservice.util.NotLoggedInException
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class WellKnownSupportApiDelegateImpl(
    private val logger: Logger,
    private val wellKnownSupportService: WellKnownSupportService
) : WellKnownSupportApiDelegate{
    override fun retrieveSupportInformation(serverName: String): ResponseEntity<ServerSupportInformation> {
        return try {
            val supportInformation = wellKnownSupportService.getSupportInformationForServerName(serverName)

            when(supportInformation){
                is ServerSupportInformation -> ResponseEntity.ok(supportInformation)
                else -> ResponseEntity.notFound().build()
            }
        } catch (e: Exception){
            logger.error("Error getting support info for $serverName, cause: ${e.localizedMessage}")
            ResponseEntity.internalServerError().build()
        }
    }

    override fun setSupportInformation(serverName: String, serverSupportInformation: ServerSupportInformation): ResponseEntity<Unit> {
        return try {
            wellKnownSupportService.setSupportInformationForServerName(
                serverName = serverName,
                supportInformation = serverSupportInformation
            )
            ResponseEntity.ok().build()
        } catch (e: NotLoggedInException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        } catch (e: IllegalArgumentException){
            ResponseEntity.badRequest().build()
        } catch (e: NotAllowedException){
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        } catch (e: Exception){
            logger.error("Error setting support info for $serverName, cause: ${e.localizedMessage}")
            ResponseEntity.internalServerError().build()
        }
    }
}
