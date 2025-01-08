/*
 * Copyright (C) 2024 akquinet GmbH
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

import de.akquinet.tim.registrationservice.openapi.api.federation.server.FederationApiDelegate
import de.akquinet.tim.registrationservice.openapi.model.federation.FederationList
import de.akquinet.tim.registrationservice.persistance.federation.FederationListRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import java.util.Optional

@Component
class FederationApiDelegateImpl @Autowired constructor(
    private val request: NativeWebRequest,
    private val federationListRepository: FederationListRepository
) : FederationApiDelegate {

    override fun retrieveFederationList(version: Long?): ResponseEntity<FederationList> {
        return version?.let {
            val federationEntity = federationListRepository.getFirstByVersionGreaterThanOrderByVersionDesc(it)
            federationEntity?.let { federationListEntity ->
                ResponseEntity.ok(federationListEntity.toModel())
            } ?: run {
                ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .body(federationListRepository.findFirstByOrderByVersionDesc().toModel())
            }
        } ?: run {
            ResponseEntity.ok(federationListRepository.findFirstByOrderByVersionDesc().toModel())
        }
    }

    override fun getRequest() = Optional.of(request)
}