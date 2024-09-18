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

package de.akquinet.tim.registrationservice.api.messengerservice

import de.akquinet.tim.registrationservice.api.operator.OperatorService
import de.akquinet.tim.registrationservice.config.RegServiceConfig
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstance
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.tim.registrationservice.util.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service

@EnableScheduling
@Service
class MessengerInstanceService @Autowired constructor(
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val userService: UserService,
    private val regServiceConfig: RegServiceConfig,
    private val operatorService: OperatorService,
) {

    companion object {
        const val ORG_ADMIN_ERROR_LOG_TEMPLATE = "Error creating org admin ({}): {}"
    }

    fun getAllInstancesForCurrentUser(): List<MessengerInstance> =
        messengerInstanceRepository.findAllByUserId(userService.getUserIdFromContext())
            .map { it.toMessengerInstance() }

    fun getInstanceState(serverName: String): InstanceStateDto {
        val instanceEntity = messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
            serverName,
            userService.getUserIdFromContext()
        )

        val httpStatus = instanceEntity?.let {
            if (regServiceConfig.callExternalServices) {
                operatorService.operatorInstanceCheck(instanceEntity.serverName)
            } else {
                HttpStatus.OK
            }
        } ?: HttpStatus.NOT_FOUND

        return when (httpStatus) {
            HttpStatus.OK -> InstanceStateDto(isReady = true)

            // description of 500 is used in frontend, please change it there as well if you are making changes here
            HttpStatus.INTERNAL_SERVER_ERROR -> InstanceStateDto(
                isReady = false,
                message = "Error during operator instance check"
            )

            HttpStatus.NOT_FOUND -> InstanceStateDto(
                isReady = false,
                message = "Messenger Instance could not be found"
            )

            else -> InstanceStateDto(
                isReady = false,
                message = "Interal Server Error in Backend during instance ready check"
            )
        }
    }
}

data class InstanceStateDto(
    val isReady: Boolean,
    val message: String? = null
)
