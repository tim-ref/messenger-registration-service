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

package de.akquinet.tim.registrationservice.api.messengerservice

import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.tim.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.slf4j.Logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.ApplicationScope
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Service
@ApplicationScope
class MessengerInstanceCheckService(
    val logger: Logger,
    val messengerInstanceRepository: MessengerInstanceRepository,
    val messengerInstanceDeleteService: MessengerInstanceDeleteService,
    val meterRegistry: MeterRegistry
) {
    val registeredGauges: ConcurrentHashMap<String, Gauge> = ConcurrentHashMap()

    @EventListener(ApplicationReadyEvent::class)
    @Scheduled(cron = "0 0 22 * * *")
    fun checkDaysToEOLOfAllInstances() {
        logger.info("Check days until EoL for all instances")
        val allInstances = messengerInstanceRepository.findAll()

        allInstances.forEach { instance ->

            val daysUntilEOL = calcDaysToEOLOfInstance(instance,LocalDate.now())
            logger.info("$daysUntilEOL until ${instance.serverName} reaches EoL")

            if (daysUntilEOL < 0) {
                messengerInstanceDeleteService.deleteInstance(instance.serverName, instance.userId)
                unregisterInstanceMetric(instance.instanceId)
            } else {
                registerMetric(instance, daysUntilEOL)
            }
        }


        val unavailableGauges = registeredGauges.filterNot {
            gaugeEntry -> allInstances.any { it.instanceId == gaugeEntry.key  }
        }

        unavailableGauges.forEach {
            unregisterInstanceMetric(it.key)
        }
    }

    fun calcDaysToEOLOfInstance(instance: MessengerInstanceEntity,now:LocalDate): Double =
        java.time.temporal.ChronoUnit.DAYS.between(now, instance.endDate).toDouble()

    private fun registerMetric(instance: MessengerInstanceEntity, daysUntilEOL: Double) {
        val gauge: Gauge = Gauge
            .builder("messenger_instance_days_until_eol") { daysUntilEOL }
            .tags( Tags.of("telematik_id", instance.telematikId))
            .tags( Tags.of("server_name", instance.serverName))
            .tags( Tags.of("instance_name", instance.instanceId))
            .register(meterRegistry)

        registeredGauges[instance.instanceId] = gauge
    }

    private fun unregisterInstanceMetric(instanceId: String){
        val meterId = registeredGauges[instanceId]?.id

        meterId?.let {
            meterRegistry.remove(it)
            registeredGauges.remove(instanceId)
        }
    }
}