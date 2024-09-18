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

package de.akquinet.tim.registrationservice.config

import de.akquinet.tim.registrationservice.rawdata.RawDataService
import de.akquinet.tim.registrationservice.rawdata.model.Operation
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class RawDataCreationFilter(private val rawdataService: RawDataService) : OncePerRequestFilter() {
    companion object {
        val ALLOWED_HTTP_METHODS = setOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
            .map { it.name() }
    }

    @ExperimentalTime
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val elapsed = measureTimedValue {
            filterChain.doFilter(request, response)
        }

        // in order the responseBody has a non 0 value the rawdataService.responseBodySize has to be set in the application
        rawdataService.collectAndSendRawData(
            request.getHeader(HttpHeaders.CONTENT_LENGTH)?.toIntOrNull() ?: 0,
            rawdataService.responseBodySize,
            HttpStatusCode.valueOf(response.status),
            elapsed.duration,
            Operation.RS_LOGIN,
            ""
        )

        // clear the body size storage
        rawdataService.responseBodySize = 0
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !shouldFilter(request)

    private fun shouldFilter(request: HttpServletRequest): Boolean {
        val requestUrl = request.requestURL.toString()
        return request.method in ALLOWED_HTTP_METHODS && requestUrl.contains("/messengerInstance")

    }
}
