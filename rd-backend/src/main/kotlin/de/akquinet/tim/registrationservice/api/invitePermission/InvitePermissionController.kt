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

package de.akquinet.tim.registrationservice.api.invitePermission

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "InvitePermission", description = "API for receiving Invite permission Checks Proxy")
@ApiResponses(
    value = [ApiResponse(
        responseCode = "500", description = "Internal Server Error", content = [Content()]
    )]
)
class InvitePermissionController(private val invitePermissionService: InvitePermissionService) {

    @PostMapping("/vzd/invite", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Retrieve Invitepermission level 3 for two Users",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "A pair of userIds in uri format",
            required = true
        )
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Returning OK if permissible"),
            ApiResponse(responseCode = "401", description = "Returning FORBIDDEN if NOT permissible")
        ]
    )
    fun getUserPublicityFromVZD(@RequestBody userPair: Pair<String, String>): ResponseEntity<String> {
        return if (invitePermissionService.checkUserInvitePermissions(userPair.first, userPair.second))
            ResponseEntity.status(HttpStatus.OK).build()
        else
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
