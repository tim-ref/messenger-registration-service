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

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.net.URI


@Configuration
class WebConfiguration(
    private val frontendConfig: FrontendConfig,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}")
    private val keycloakUrl: URI?,
    private val applicationConfig: ApplicationConfig,
) {


    @Bean
    fun corsConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {

            registry.addMapping("/**")
                .allowedMethods("*")
                // keycloakUrl.resolve("/") strips the path from the url
                .allowedOrigins(
                    frontendConfig.host,
                    keycloakUrl?.resolve("/").toString(),
                    *applicationConfig.orgAdminOrigins.toTypedArray()
                )
        }
    }
}
