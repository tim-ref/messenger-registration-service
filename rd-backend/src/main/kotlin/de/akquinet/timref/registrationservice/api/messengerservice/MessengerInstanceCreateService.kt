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

import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MessengerInstanceCreateService @Autowired constructor(
    private val messengerInstanceRepository: MessengerInstanceRepository
) {

    fun randomInstanceName(): String = List(INSTANCE_NAME_LENGTH) { CHARACTER_POOL.random() }.joinToString("")

    fun generateAvailableInstanceName(baseFQDN: String): String {
        for (i in 0..<MAX_ROUNDS) {
            val result = randomInstanceName()
            val fqdn = result + baseFQDN
            if (messengerInstanceRepository.findAllByServerNameOrPublicBaseUrl(fqdn, fqdn).isEmpty()) {
                return result
            }
        }

        throw IllegalStateException("Could not find an available instance name (tries=$MAX_ROUNDS)")
    }

    companion object {
        const val MAX_ROUNDS = 100
        const val INSTANCE_NAME_LENGTH: Int = 3
        val CHARACTER_POOL: List<Char> = ('a'..'z') + ('0'..'9')
        const val X_HEADER_INSTANCE_RANDOM = "x-inran"
    }
}
