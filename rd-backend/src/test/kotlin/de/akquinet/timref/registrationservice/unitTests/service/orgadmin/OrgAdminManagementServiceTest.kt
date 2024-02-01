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

package de.akquinet.timref.registrationservice.unitTests.service.orgadmin

import de.akquinet.timref.registrationservice.persistance.orgAdmin.OrgAdminRepository
import de.akquinet.timref.registrationservice.persistance.orgAdmin.model.OrgAdminEntity
import de.akquinet.timref.registrationservice.service.orgadmin.OrgAdminManagementService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OrgAdminManagementServiceTest: DescribeSpec() {

    private val attachedEntity = OrgAdminEntity(
        id = UUID.fromString("2c151ae4-ee33-4c85-a59d-b08582de6117"),
        telematikId = "12345-akq",
        mxId = "someone@localhost",
        professionOid = "1.2.3.4.5",
        serverName = "localhost"
    )

    init {
        describe("OrgAdminManagementServiceTest") {
            it("createOrgAdmin returns attached entity") {
                val repo: OrgAdminRepository = mockk {
                    every { save(any()) } returns attachedEntity
                }

                val subjectUnderTest = OrgAdminManagementService(repo = repo)

                val result = subjectUnderTest.createOrgAdmin(
                    telematikId = "12345-akq",
                    mxId = "someone@localhost",
                    professionOid = "1.2.3.4.5",
                    serverName = "localhost"
                )

                result shouldBe attachedEntity
            }

            it("deleteOrgAdmin works") {
                val mxId = "someone@localhost"
                val repo: OrgAdminRepository = mockk {
                    every { getByMxId(mxId) } returns  attachedEntity
                    every { delete(attachedEntity) } answers {}
                }

                val subjectUnderTest = OrgAdminManagementService(repo = repo)

                subjectUnderTest.deleteOrgAdmin(mxId = mxId)

                verify { repo.delete(attachedEntity) }
            }

            it("deleteOrgAdmin throws exception when orgadmin does not exist") {
                val mxId = "someone@localhost"
                val repo: OrgAdminRepository = mockk {
                    every { getByMxId(mxId) } returns null
                }

                val subjectUnderTest = OrgAdminManagementService(repo = repo)

                shouldThrow<OrgAdminManagementService.OrgAdminDoesNotExistException> { subjectUnderTest.deleteOrgAdmin(mxId = mxId) }
            }

            it("getByMxId calls repo") {
                val mxId = attachedEntity.mxId.toString()
                val repo: OrgAdminRepository = mockk {
                    every { getByMxId(mxId) } returns attachedEntity
                }

                val subjectUnderTest = OrgAdminManagementService(repo = repo)

                subjectUnderTest.getByMxId(mxId) shouldBe attachedEntity
                verify { repo.getByMxId(mxId) }
            }
        }
    }
}
