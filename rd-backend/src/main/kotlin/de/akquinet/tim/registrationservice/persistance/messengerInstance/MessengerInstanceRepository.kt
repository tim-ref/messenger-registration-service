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

package de.akquinet.tim.registrationservice.persistance.messengerInstance

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Transactional()
interface MessengerInstanceRepository : JpaRepository<MessengerInstanceEntity, String> {

    fun findDistinctFirstByServerNameAndUserId(serverName: String, userId: String): MessengerInstanceEntity?
    fun findAllByUserId(userId: String): List<MessengerInstanceEntity>

    fun findByServerName(serverName: String): MessengerInstanceEntity?
    fun findAllByServerNameOrPublicBaseUrl(serverName: String, publicBaseUrl: String): List<MessengerInstanceEntity>

    fun findAllByServerNameIsInAndActive(inactiveList: List<String>, active: Boolean): List<MessengerInstanceEntity>
    fun findAllByServerNameIsNotInAndActive(inactiveList: List<String>, active: Boolean): List<MessengerInstanceEntity>

    fun findAllByStartOfInactivityLessThan(start: Long): List<MessengerInstanceEntity>


}
