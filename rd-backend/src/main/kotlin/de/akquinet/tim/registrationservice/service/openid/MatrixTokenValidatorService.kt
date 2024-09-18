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

package de.akquinet.tim.registrationservice.service.openid

import de.akquinet.tim.registrationservice.config.MatrixConfig
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.net.URLEncoder

private fun String.urlEncoded() = URLEncoder.encode(this, Charsets.UTF_8)

internal class InvalidTokenException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

@Component
class MatrixTokenValidatorService(
    private val logger: Logger,
    private val config: MatrixConfig,
    private val restTemplate: RestTemplate
) {

    fun validateToken(userId: String, requestToken: String, synapseServerName: String): String {
        val subject: String = getSubjectViaTokenIntrospection(requestToken, synapseServerName)

        if (userId == subject) {
            return userId
        } else {
            logger.error("A user was trying to obtain a regservice openid token for the mx id '$userId' using a token which according to the matrix server belongs to '$subject'. Rejecting this request!")
            throw InvalidTokenException("Unauthorized")
        }
    }

    private fun getSubjectViaTokenIntrospection(requestToken: String, synapseServerName: String): String {
        val response = try {
            restTemplate.getForEntity(
                URI("${config.serverScheme}://$synapseServerName:${config.serverPort}/_matrix/federation/v1/openid/userinfo?access_token=${requestToken.urlEncoded()}"),
                Map::class.java
            )
        } catch (e: HttpClientErrorException) {
            logger.error("Could not retrieve userinfo from matrix server, got status ${e.statusCode.value()}", e)
            throw InvalidTokenException("Could not retrieve userinfo from matrix server", e)
        }

        val body = response.body
        if (body != null) {
            val token = body["sub"]
            if (token != null && token is String) {
                return token
            } else {
                val msg =
                    "Could not retrieve userinfo from matrix server. Got status ${response.statusCode}. The body was missing the field 'sub' or it was not a string."
                logger.error(msg)
                throw RuntimeException(msg)
            }
        } else {
            val msg =
                "Could not retrieve userinfo from matrix server. Got status ${response.statusCode}. Body was null."
            logger.error(msg)
            throw RuntimeException(msg)
        }
    }

}
