/*
 * Copyright (C) 2023-2024 akquinet GmbH
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

package de.akquinet.tim.registrationservice.persistance.federation

import de.akquinet.tim.registrationservice.persistance.federation.model.FederationListEntity
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Transactional
interface FederationListRepository : JpaRepository<FederationListEntity, String> {

    fun deleteAllByVersionNot(version: Long)
    fun findFirstByOrderByVersionDesc(): FederationListEntity
    fun getFirstByVersionGreaterThanOrderByVersionDesc(version: Long): FederationListEntity?
}
