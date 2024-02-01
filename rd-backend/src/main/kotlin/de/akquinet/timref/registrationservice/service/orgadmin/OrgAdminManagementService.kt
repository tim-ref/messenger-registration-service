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

package de.akquinet.timref.registrationservice.service.orgadmin

import de.akquinet.timref.registrationservice.persistance.orgAdmin.OrgAdminRepository
import de.akquinet.timref.registrationservice.persistance.orgAdmin.model.OrgAdminEntity
import org.springframework.stereotype.Service

@Service
class OrgAdminManagementService(
    private val repo: OrgAdminRepository
) {
    class OrgAdminDoesNotExistException : RuntimeException()

    fun createOrgAdmin(telematikId: String, mxId: String, professionOid: String, serverName: String): OrgAdminEntity = repo
        .save(
            OrgAdminEntity(
                telematikId = telematikId,
                mxId = mxId,
                professionOid = professionOid,
                serverName = serverName
            )
        )

    fun getByMxId(mxId: String): OrgAdminEntity? = repo.getByMxId(mxId = mxId)

    fun getByServerName(serverName: String): OrgAdminEntity? = repo.getByServerName(name = serverName)

    fun delete(entity: OrgAdminEntity) = repo.delete(entity)

    fun deleteOrgAdmin(mxId: String) {
        val entity = repo.getByMxId(mxId = mxId)

        if (entity != null) {
            repo.delete(entity)
        } else {
            throw OrgAdminDoesNotExistException()
        }
    }
}
