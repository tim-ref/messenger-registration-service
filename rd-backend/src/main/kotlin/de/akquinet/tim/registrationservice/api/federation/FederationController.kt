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

package de.akquinet.tim.registrationservice.api.federation

import com.google.gson.Gson
import de.akquinet.tim.registrationservice.api.federation.model.FederationListResponse
import de.akquinet.tim.registrationservice.persistance.federation.FederationRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["federation"], produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "Federation", description = "API for updating the federation list")
@ApiResponses(
    value = [ApiResponse(
        responseCode = "500", description = "Internal Server Error", content = [Content()]
    )]
)
class FederationController(
    val federationRepository: FederationRepository
) {
    private val gson = Gson()

    @GetMapping
    @Operation(summary = "Check if a new federation list is available and if true return it.")
    @ApiResponses(
        value = [ApiResponse(
            description = "The provided federation list is up-to-date", responseCode = "304", content = [Content()]
        ), ApiResponse(description = "Returning the updated federation list", responseCode = "200")]
    )
    fun retrieveFederationList(): ResponseEntity<String> {
           return FederationListResponse(HttpStatus.OK, federationRepository.findFirstByOrderByVersionDesc().toModel())
                .toResponseEntity(gson)
    }
}
