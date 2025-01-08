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

package de.akquinet.tim.registrationservice.unitTests.api.messengerservice.model

import de.akquinet.tim.registrationservice.openapi.model.mi.GetMessengerInstanceTimAuthConceptConfig200Response
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfigurationProxy
import de.akquinet.tim.registrationservice.api.messengerservice.model.getOverrideConfigurationProxyFromResponseModel
import de.akquinet.tim.registrationservice.api.messengerservice.model.getOverrideConfigurationResponseFromModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OperatorOverrideConverterTest {

    @Test
    fun `test getOverrideConfigurationProxyFromResponseModel with PROXY and ALLOW_ALL`() {
        val responseModel = GetMessengerInstanceTimAuthConceptConfig200Response(
            federationCheckConcept = GetMessengerInstanceTimAuthConceptConfig200Response.FederationCheckConcept.PROXY,
            inviteRejectionPolicy = GetMessengerInstanceTimAuthConceptConfig200Response.InviteRejectionPolicy.ALLOW_ALL
        )

        val result = getOverrideConfigurationProxyFromResponseModel(responseModel)

        assertEquals(SynapseOverrideConfigurationProxy.FederationCheckConcept.PROXY, result.federationCheckConcept)
        assertEquals(SynapseOverrideConfigurationProxy.InviteRejectionPolicy.ALLOW_ALL, result.inviteRejectionPolicy)
    }

    @Test
    fun `test getOverrideConfigurationProxyFromResponseModel with CLIENT and BLOCK_ALL`() {
        val responseModel = GetMessengerInstanceTimAuthConceptConfig200Response(
            federationCheckConcept = GetMessengerInstanceTimAuthConceptConfig200Response.FederationCheckConcept.CLIENT,
            inviteRejectionPolicy = GetMessengerInstanceTimAuthConceptConfig200Response.InviteRejectionPolicy.BLOCK_ALL
        )

        val result = getOverrideConfigurationProxyFromResponseModel(responseModel)

        assertEquals(SynapseOverrideConfigurationProxy.FederationCheckConcept.CLIENT, result.federationCheckConcept)
        assertEquals(SynapseOverrideConfigurationProxy.InviteRejectionPolicy.BLOCK_ALL, result.inviteRejectionPolicy)
    }

    @Test
    fun `test getOverrideConfigurationProxyFromResponseModel with null values`() {
        val responseModel = GetMessengerInstanceTimAuthConceptConfig200Response(
            federationCheckConcept = null,
            inviteRejectionPolicy = null
        )

        val result = getOverrideConfigurationProxyFromResponseModel(responseModel)

        assertNull(result.federationCheckConcept)
        assertNull(result.inviteRejectionPolicy)
    }

    @Test
    fun `test getOverrideConfigurationResponseFromModel with PROXY and ALLOW_ALL`() {
        val proxyModel = SynapseOverrideConfigurationProxy(
            federationCheckConcept = SynapseOverrideConfigurationProxy.FederationCheckConcept.PROXY,
            inviteRejectionPolicy = SynapseOverrideConfigurationProxy.InviteRejectionPolicy.ALLOW_ALL
        )

        val result = getOverrideConfigurationResponseFromModel(proxyModel)

        assertEquals(GetMessengerInstanceTimAuthConceptConfig200Response.FederationCheckConcept.PROXY, result.federationCheckConcept)
        assertEquals(GetMessengerInstanceTimAuthConceptConfig200Response.InviteRejectionPolicy.ALLOW_ALL, result.inviteRejectionPolicy)
    }

    @Test
    fun `test getOverrideConfigurationResponseFromModel with CLIENT and BLOCK_ALL`() {
        val proxyModel = SynapseOverrideConfigurationProxy(
            federationCheckConcept = SynapseOverrideConfigurationProxy.FederationCheckConcept.CLIENT,
            inviteRejectionPolicy = SynapseOverrideConfigurationProxy.InviteRejectionPolicy.BLOCK_ALL
        )

        val result = getOverrideConfigurationResponseFromModel(proxyModel)

        assertEquals(GetMessengerInstanceTimAuthConceptConfig200Response.FederationCheckConcept.CLIENT, result.federationCheckConcept)
        assertEquals(GetMessengerInstanceTimAuthConceptConfig200Response.InviteRejectionPolicy.BLOCK_ALL, result.inviteRejectionPolicy)
    }

    @Test
    fun `test getOverrideConfigurationResponseFromModel with null values`() {
        val proxyModel = SynapseOverrideConfigurationProxy(
            federationCheckConcept = null,
            inviteRejectionPolicy = null
        )

        val result = getOverrideConfigurationResponseFromModel(proxyModel)

        // Default values when null are used for federationCheckConcept (CLIENT) and inviteRejectionPolicy (ALLOW_ALL)
        assertEquals(GetMessengerInstanceTimAuthConceptConfig200Response.FederationCheckConcept.CLIENT, result.federationCheckConcept)
        assertEquals(GetMessengerInstanceTimAuthConceptConfig200Response.InviteRejectionPolicy.ALLOW_ALL, result.inviteRejectionPolicy)
    }
}
