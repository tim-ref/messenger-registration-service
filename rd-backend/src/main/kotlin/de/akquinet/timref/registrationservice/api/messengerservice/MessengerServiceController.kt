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

import com.google.gson.Gson
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstance
import de.akquinet.timref.registrationservice.rawdata.RawDataServiceImpl
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.ExperimentalTime


@RestController
@RequestMapping(
    path = ["messengerInstance"], produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "Messenger-Service", description = "API for creating a new messenger service instance")
@ApiResponses(
    value = [ApiResponse(
        responseCode = "500", description = "Internal Server Error", content = [Content()]
    )]
)
class MessengerServiceController(
    private val messengerInstanceService: MessengerInstanceServiceImpl,
    private val rawDataService: RawDataServiceImpl
) {
    private val gson = Gson()

    @GetMapping
    @Operation(summary = "Gets all Instances for a User.")
    @ApiResponses(
        value = [ApiResponse(description = "Retrieved all instances for the user", responseCode = "200")]
    )
    fun get(): ResponseEntity<List<MessengerInstance>> {
        val responseEntity = ResponseEntity(messengerInstanceService.getAllInstancesForUser(), HttpStatus.OK)
        rawDataService.responseBodySize = gson.toJson(responseEntity.body).length
        return responseEntity
    }

    @OptIn(ExperimentalTime::class)
    @PostMapping("/create")
    @Operation(summary = "Creates a new messenger service with given parameter.")
    @ApiResponses(
        value = [
            ApiResponse(description = "A new messenger service instance is successfully created", responseCode = "201"),
            ApiResponse(description = "The user was not found", responseCode = "404"),
            ApiResponse(description = "A input value contains wrong characters", responseCode = "400"),
            ApiResponse(
                description = "An instance with the selected server name or base URL already exists",
                responseCode = "409"
            ),
            ApiResponse(description = "Error on creating new instance through operator", responseCode = "500")
        ]
    )
    fun createMessengerService(
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<String> {
        val responseEntity = messengerInstanceService.createNewInstance(httpServletRequest)
        rawDataService.responseBodySize = responseEntity.body?.length ?: 0
        return responseEntity
    }

    @DeleteMapping("/{serverName}/")
    @Operation(summary = "Deletes a messenger service with given parameter.")
    @ApiResponses(
        value = [ApiResponse(
            description = "A new messenger service instance is successfully deleted",
            responseCode = "200"
        ),
            ApiResponse(description = "A input value contains wrong characters", responseCode = "400"),
            ApiResponse(description = "Messenger Instance not found", responseCode = "404"),
            ApiResponse(description = "Error on deleting new instance through operator", responseCode = "500")]
    )
    fun deleteMessengerService(@PathVariable serverName: String): ResponseEntity<String> {
        val responseEntity = messengerInstanceService.deleteInstance(serverName)
        rawDataService.responseBodySize = responseEntity.body?.length ?: 0
        return responseEntity
    }


    @GetMapping("/{serverName}/admin")
    @Operation(summary = "First Time creation of Admin User")
    @ApiResponses(
        value = [ApiResponse(description = "An admin User has been successfully created", responseCode = "201"),
            ApiResponse(description = "Messenger Instance not found", responseCode = "404"),
            ApiResponse(description = "Instance is not ready yet", responseCode = "423"),
            ApiResponse(description = "Admin user for for Instance already Exists", responseCode = "409"),
            ApiResponse(description = "Error on creating admin user through operator", responseCode = "500")]
    )
    fun createAdminUser(@PathVariable serverName: String): ResponseEntity<String> {
        return if (messengerInstanceService.instanceReadyCheck(serverName).statusCode == HttpStatus.OK)
            messengerInstanceService.createAdminUser(serverName)
        else
            ResponseEntity("Instance is not ready yet", HttpStatus.LOCKED)
    }

    @PostMapping("/{serverName}/loglevel")
    @Operation(summary = "Change Loglevel")
    @ApiResponses(
        value = [ApiResponse(description = "Loglevel has been successfully changed", responseCode = "200"),
            ApiResponse(description = "Messenger Instance not found", responseCode = "404"),
            ApiResponse(description = "Error changing logLevel through operator", responseCode = "500")]
    )
    fun changeInstanceLogLevel(@PathVariable serverName: String, @RequestBody logLevel: String): ResponseEntity<String> {
        return messengerInstanceService.changeLogLevel(serverName, logLevel)
    }

}

