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

import de.akquinet.tim.registrationservice.openapi.operator.client.SynapseOperatorApi
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.apache.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OperatorConfig @Autowired constructor(
    val properties: Properties
) {

    @Bean
    fun operatorApiClient(): SynapseOperatorApi {
        val basePath = "${properties.host}:${properties.port}"

        val credentials = Credentials.basic(
            properties.credentials.username,
            properties.credentials.password
        )
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val authenticatedRequest = request
                    .newBuilder()
                    .header(HttpHeaders.AUTHORIZATION, credentials)
                    .build()
                chain.proceed(authenticatedRequest)
            }.build()

        return SynapseOperatorApi(basePath, httpClient)
    }

    @ConfigurationProperties(prefix = "operator")
    data class Properties(
        val host: String,
        val port: String,
        val createPath: String,
        val deletePath: String,
        val baseFQDN: String,
        val credentials: Credentials
    ) {
        @ConfigurationProperties(prefix = "operator.credentials")
        data class Credentials(
            val username: String,
            val password: String
        )
    }
}
