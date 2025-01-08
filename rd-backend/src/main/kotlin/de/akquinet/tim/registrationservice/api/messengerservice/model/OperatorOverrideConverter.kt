package de.akquinet.tim.registrationservice.api.messengerservice.model

import de.akquinet.tim.registrationservice.openapi.model.mi.GetMessengerInstanceTimAuthConceptConfig200Response
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfigurationProxy
import de.akquinet.tim.registrationservice.openapi.model.mi.GetMessengerInstanceTimAuthConceptConfig200Response.FederationCheckConcept as ResponseConcept
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfigurationProxy.FederationCheckConcept as SynapseConcept

import de.akquinet.tim.registrationservice.openapi.model.mi.GetMessengerInstanceTimAuthConceptConfig200Response.InviteRejectionPolicy as ResponsePolicy
import de.akquinet.tim.registrationservice.openapi.model.operator.SynapseOverrideConfigurationProxy.InviteRejectionPolicy as SynapsePolicy


// Maps the response 200 models to the according model. This is necessary because the Open Api Generator produces a seperate model for http responses.
fun getOverrideConfigurationProxyFromResponseModel(model: GetMessengerInstanceTimAuthConceptConfig200Response): SynapseOverrideConfigurationProxy {
    val federationCheckConcept = when (model.federationCheckConcept) {
        ResponseConcept.PROXY -> SynapseConcept.PROXY
        ResponseConcept.CLIENT -> SynapseConcept.CLIENT
        null -> null
    }
    val inviteRejectionPolicy = when (model.inviteRejectionPolicy) {
        ResponsePolicy.ALLOW_ALL -> SynapsePolicy.ALLOW_ALL
        ResponsePolicy.BLOCK_ALL -> SynapsePolicy.BLOCK_ALL
        null -> null
    }
    return SynapseOverrideConfigurationProxy(
        federationCheckConcept = federationCheckConcept,
        inviteRejectionPolicy = inviteRejectionPolicy
    )
}


fun getOverrideConfigurationResponseFromModel(model: SynapseOverrideConfigurationProxy): GetMessengerInstanceTimAuthConceptConfig200Response {
    val federationCheckConcept = when (model.federationCheckConcept) {
        SynapseConcept.PROXY -> ResponseConcept.PROXY
        SynapseConcept.CLIENT -> ResponseConcept.CLIENT
        // Client is the default, if no override value was set in the past
        null -> ResponseConcept.CLIENT
    }
    val inviteRejectionPolicy = when (model.inviteRejectionPolicy) {
        SynapsePolicy.ALLOW_ALL -> ResponsePolicy.ALLOW_ALL
        SynapsePolicy.BLOCK_ALL -> ResponsePolicy.BLOCK_ALL
        // Allow all is the default, if no override value was set in the past
        null -> ResponsePolicy.ALLOW_ALL
    }
    return GetMessengerInstanceTimAuthConceptConfig200Response(
        federationCheckConcept = federationCheckConcept,
        inviteRejectionPolicy = inviteRejectionPolicy
    )
}



