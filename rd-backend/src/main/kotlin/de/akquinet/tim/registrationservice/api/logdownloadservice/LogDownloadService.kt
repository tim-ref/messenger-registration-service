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

package de.akquinet.tim.registrationservice.api.logdownloadservice

import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.InputStream

sealed interface LogDownloadResult {
    data class Unauthorized(val serverName: String) : LogDownloadResult
    data class InvalidInput(val hint: String) : LogDownloadResult
    data class Unexpected(val hint: String) : LogDownloadResult
    data class LogStream(val inputStream: InputStream, val numberOfBytes: Int) : LogDownloadResult
}

private fun String.toInputStreamResource() = InputStreamResource(this.byteInputStream())

internal fun LogDownloadResult.toResponseEntity(): ResponseEntity<InputStreamResource> =
    when (this) {
        is LogDownloadResult.Unauthorized ->
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    "not an instance: $serverName".toInputStreamResource()
                )

        is LogDownloadResult.InvalidInput ->
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("invalid input: $hint".toInputStreamResource()
                )

        is LogDownloadResult.Unexpected ->
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("an unexpected response occurred: $hint".toInputStreamResource()
            )

        is LogDownloadResult.LogStream ->
            ResponseEntity
                .ok()
                .header("Content-Length", numberOfBytes.toString())
                .header("Content-Disposition", "attachment")
                .header("Content-Type", "text/plain")
                .body(InputStreamResource(inputStream))
    }

@Service
interface LogDownloadService {
    fun getLogs(
        serverName: String,
        containerName: String,
        start: Long? = null,
        end: Long? = null
    ): LogDownloadResult
}