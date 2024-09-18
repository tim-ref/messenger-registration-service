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

import de.akquinet.tim.registrationservice.util.TrivialResponseErrorHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class RestTemplateConfig @Autowired constructor(
    private val operatorConfig: OperatorConfig
) {

    @Bean
    @Primary
    fun restTemplatePrimary(
        builder: RestTemplateBuilder
    ): RestTemplate = builder.errorHandler(TrivialResponseErrorHandler()).build()

    @Bean
    @Qualifier("operator")
    fun restTemplateOperator(
        builder: RestTemplateBuilder
    ): RestTemplate = builder
        .errorHandler(TrivialResponseErrorHandler())
        .basicAuthentication(
            operatorConfig.properties.credentials.username,
            operatorConfig.properties.credentials.password
        ).build()

    @Bean
    @Qualifier("healthIndicator")
    fun restTemplateHealthIndicator(
        builder: RestTemplateBuilder
    ): RestTemplate = builder
        .setConnectTimeout(Duration.ofMillis(500))
        .setReadTimeout(Duration.ofMillis(500))
        .build()
}
