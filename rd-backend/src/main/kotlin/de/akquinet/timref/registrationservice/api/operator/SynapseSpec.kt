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

package de.akquinet.timref.registrationservice.api.operator


data class SynapseSpec(
    val proxyConfig: ProxyConfig?,
    val synapseConfig: SynapseConfig?,
    val logLevel: String
)

data class ProxyConfig(
    val rawDataIDs: RawDataIDs,
)

data class RawDataIDs(
    val instanceID: String,
    val telematikID: String,
    val professionOID: String,
)

data class SynapseConfig(
    val homeServer: HomeServer,
    val singleSignOn: SingleSignOn?
)

data class HomeServer(
    val serverName: String,
    val publicBaseUrl: String
)

data class AdminUser(
    val userName: String,
    val password: String
)

data class SynapseResponse(
    val accessToken: String? = null,
    val deviceId: String? = null,
    val homeServer: String? = null,
    val userId: String? = null
)

data class SingleSignOn(val oidcProviders: List<OidcProvider>)

data class OidcProvider(
    val idpId: String = "keycloak",
    val idpName: String = "Keycloak SingleSignOn",
    val issuer: String,
    val clientID: String = "synapse",
    val clientSecret: String,
    val scopes: List<String> = listOf("openid", "profile"),
    val userMappingProvider: UserMappingProvider,
)

data class UserMappingProvider(
    val config: UserMappingConfig,
)

data class UserMappingConfig(
    val localPartTemplate: String = "{{ user.preferred_username }}",
    val displayNameTemplate: String = "{{ user.name }}"
)
