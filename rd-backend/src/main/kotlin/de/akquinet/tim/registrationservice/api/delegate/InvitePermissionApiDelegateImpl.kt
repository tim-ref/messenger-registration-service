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

import de.akquinet.tim.registrationservice.api.invitePermission.InvitePermissionService
import de.akquinet.tim.registrationservice.openapi.api.invitepermission.server.InvitePermissionApiDelegate
import de.akquinet.tim.registrationservice.openapi.model.invitepermission.InvitePermissionDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import java.util.Optional

@Component
class InvitePermissionApiDelegateImpl @Autowired constructor(
    private val request: NativeWebRequest,
    private val invitePermissionService: InvitePermissionService
) : InvitePermissionApiDelegate {

    override fun getRequest() = Optional.of(request)

    override fun getUserPublicityFromVZD(invitePermissionDto: InvitePermissionDto): ResponseEntity<Unit> =
        if (invitePermissionService.checkUserInvitePermissions(
                invitePermissionDto.invitingUser, invitePermissionDto.invitedUser
            )
        ) {
            ResponseEntity.status(HttpStatus.OK).build()
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

}
