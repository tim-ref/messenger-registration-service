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

package de.akquinet.timref.registrationservice.api.messengerservice

import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstance
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity

interface MessengerInstanceService {
    fun createNewInstance(httpServletRequest: HttpServletRequest): ResponseEntity<String>
    fun deleteInstance(serverName: String, userId: String? = ""): ResponseEntity<String>

    fun getAllInstancesForUser(): List<MessengerInstance>
    fun createAdminUser(serverName: String): ResponseEntity<String>
    fun changeLogLevel(serverName: String, logLevel: String): ResponseEntity<String>
    fun instanceReadyCheck(serverName: String): ResponseEntity<String>
}
