/*
 * Copyright (C) 2023 - 2024 akquinet GmbH
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain

const val REGSERVICE_OPENID_TOKEN_PATH = "/regservice/openid/user/*/requesttoken"
const val INVITE_PERMISSION_PATH = "/vzd/invite"
private const val OPERATOR_INSTANCE_PATH = "/operator/**"
private const val OPERATOR_ROLE = "OPERATOR"


@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
class WebSecurityConfig(
    private val rawdataService: RawDataService,
    private val operatorProperties: OperatorConfig.Properties,
    private val basicAuthEntryPoint: AuthenticationEntryPoint
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        // no csrf for regservice openid token
        .csrf { it.ignoringRequestMatchers(
            REGSERVICE_OPENID_TOKEN_PATH,
            INVITE_PERMISSION_PATH,
            OPERATOR_INSTANCE_PATH
        ) }
        .cors {}

        .authorizeHttpRequests {
            it.apply {
                requestMatchers(HttpMethod.POST, OPERATOR_INSTANCE_PATH).hasRole(OPERATOR_ROLE)
                requestMatchers(HttpMethod.DELETE, OPERATOR_INSTANCE_PATH).hasRole(OPERATOR_ROLE)
            }
        }
        .httpBasic { it.authenticationEntryPoint(basicAuthEntryPoint) }

        // add custom filter before authentication to create performance data of unsuccessful login tries
        .addFilterBefore(
            RawDataCreationFilter(rawdataService),
            BearerTokenAuthenticationFilter::class.java
        ).authorizeHttpRequests {
            it.apply {
                requestMatchers(
                    HttpMethod.GET,
                    "/federation",
                    "/regservice/openid/cert",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/**"
                ).permitAll()
                requestMatchers(HttpMethod.POST, REGSERVICE_OPENID_TOKEN_PATH).permitAll()
                requestMatchers(HttpMethod.POST, INVITE_PERMISSION_PATH).permitAll()

                requestMatchers(HttpMethod.POST, "/**").authenticated()
                requestMatchers(HttpMethod.PUT, "/**").authenticated()
                requestMatchers(HttpMethod.DELETE, "/**").authenticated()
                requestMatchers(HttpMethod.GET, "/**").authenticated()

                anyRequest().authenticated()
            }
        }
        .oauth2ResourceServer { oauth2 -> oauth2.jwt(Customizer.withDefaults())}
        .build()

    @Bean
    fun userDetailsService() = InMemoryUserDetailsManager(
        User.builder()
            .username(operatorProperties.messengerInstanceApi.username)
            .password(passwordEncoder().encode(operatorProperties.messengerInstanceApi.password))
            .roles(OPERATOR_ROLE)
            .build()
    )

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder(8)
}
