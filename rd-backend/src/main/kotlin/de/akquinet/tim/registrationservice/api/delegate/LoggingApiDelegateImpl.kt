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

import de.akquinet.tim.registrationservice.api.logdownloadservice.LogDownloadService
import de.akquinet.tim.registrationservice.api.logdownloadservice.toResponseEntity
import de.akquinet.tim.registrationservice.api.messengerproxy.MessengerProxyLogLevelService
import de.akquinet.tim.registrationservice.openapi.api.logging.server.LoggingApiDelegate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class LoggingApiDelegateImpl @Autowired constructor(
    private val logLevelService: MessengerProxyLogLevelService,
    private val logDownloadService: LogDownloadService
): LoggingApiDelegate {

    override fun changeInstanceLogLevel(instanceId: String, body: String): ResponseEntity<Unit> {
        val response = logLevelService.changeLogLevel(instanceId, body)
        return ResponseEntity.status(response.statusCode).build()
    }
    override fun getLogs(
        serverName: String,
        containerName: String,
        start: Long?,
        end: Long?
    ): ResponseEntity<InputStreamResource> =
        logDownloadService.getLogs(serverName, containerName, start, end).toResponseEntity()
}