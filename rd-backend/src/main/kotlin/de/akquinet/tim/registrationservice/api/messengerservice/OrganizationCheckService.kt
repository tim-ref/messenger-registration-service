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

package de.akquinet.tim.registrationservice.api.messengerservice

import com.google.gson.Gson
import de.akquinet.tim.registrationservice.api.federation.FederationListServiceImpl
import de.akquinet.tim.registrationservice.config.VZDConfig
import de.akquinet.tim.registrationservice.openapi.model.federation.Domain
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import org.slf4j.Logger
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.time.Instant

@EnableScheduling
@Service
class OrganizationCheckService(
    private val logger: Logger,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val federationService: FederationListServiceImpl,
    private val vzdConfig: VZDConfig
) {
    private val gson = Gson()

    @Scheduled(cron = "\${backend.federationCheck.cron}", zone = "Europe/Berlin")
    fun getInactiveFederations() {
        logger.debug("starting federationcheck")
        val uri = vzdConfig.serviceUrl + vzdConfig.federationCheckPath
        val connection: HttpURLConnection = federationService.connectToVzd(uri, null)
        if (connection.responseCode == 200) {
            val inactiveListJson = BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
            val inactiveNameList = gson.fromJson<List<Domain>>(inactiveListJson, Domain::class.java).map { it.domain }
            deactivateInstances(inactiveNameList)
            reactivateInstances(inactiveNameList)
            removeLongInactiveFromVZD()
        }

    }

    private fun deactivateInstances(list: List<String>) {
        val instancesToDeactivate = messengerInstanceRepository.findAllByServerNameIsInAndActive(list, true)
        instancesToDeactivate.forEach {
            it.active = false
            if (it.startOfInactivity == null) {
                it.startOfInactivity = Instant.now().epochSecond
            }
            messengerInstanceRepository.save(it)
        }
    }

    private fun reactivateInstances(list: List<String>) {
        val instancesToReactivate = messengerInstanceRepository.findAllByServerNameIsNotInAndActive(list, false)
        instancesToReactivate.forEach {
            if (it.startOfInactivity == null || it.startOfInactivity!! > (Instant.now().epochSecond - 2592000)) {
                it.active = true
                it.startOfInactivity = null
                messengerInstanceRepository.save(it)
            }
        }
    }

    private fun removeLongInactiveFromVZD() {
        val instancesToRemove = messengerInstanceRepository.findAllByStartOfInactivityLessThan(Instant.now().epochSecond - 2592000)
        instancesToRemove.forEach {
            if (messengerInstanceRepository.findByServerName(it.serverName) != null)
                federationService.deleteDomainFromVzd(it.serverName)
        }
    }

}
