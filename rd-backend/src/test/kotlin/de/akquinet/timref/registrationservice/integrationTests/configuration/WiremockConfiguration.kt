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

package de.akquinet.timref.registrationservice.integrationTests.configuration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.google.gson.Gson
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.delete
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.like
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.put
import com.marcinziolo.kotlin.wiremock.returns
import de.akquinet.timref.registrationservice.api.federation.Token
import de.akquinet.timref.registrationservice.api.federation.model.Domain
import de.akquinet.timref.registrationservice.api.messengerservice.RawDataServiceConfig
import de.akquinet.timref.registrationservice.config.KeycloakAdminConfig
import de.akquinet.timref.registrationservice.config.MessengerProxyConfig
import de.akquinet.timref.registrationservice.config.OperatorConfig
import de.akquinet.timref.registrationservice.config.VZDConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus

@TestConfiguration
internal class WiremockConfiguration(
    val rawDataServiceConfig: RawDataServiceConfig,
    val vzdConfig: VZDConfig,
    val operatorConfig: OperatorConfig,
    val keycloakAdminConfig: KeycloakAdminConfig,
    val proxyConfig: MessengerProxyConfig
) {
    private val gson = Gson()


    @Bean
    @Qualifier("Rawdata")
    fun rawdataWireMockServer(): WireMockServer {
        val rawdataWireMock = WireMockServer(
            WireMockConfiguration.options().port(rawDataServiceConfig.port))

        // Endpoint: Sends performance data to rawdata master
        rawdataWireMock.post {
            urlPath equalTo rawDataServiceConfig.path
        } returns {
            statusCode = HttpStatus.OK.value()
        }

        return rawdataWireMock
    }

    @Bean
    @Qualifier("Operator")
    fun operatorWireMockServer(): WireMockServer {
        val operatorWireMock = WireMockServer(
            WireMockConfiguration.options().port(operatorConfig.port.toInt()))

        val instanceNameRegex = "[a-zA-Z0-9\\-_]*"

        // Endpoint: Create new instance through operator
        operatorWireMock.post {
            urlPath equalTo operatorConfig.createPath
        } returns {
            statusCode = HttpStatus.CREATED.value()
        }

        // Endpoint: Create new admin user
        operatorWireMock.post {
            urlPath like "${operatorConfig.createPath}/${instanceNameRegex}/admin"
        } returns {
            statusCode = HttpStatus.CREATED.value()
        }

        // Endpoint: Delete instance through operator
        operatorWireMock.delete {
            urlPath like "${operatorConfig.deletePath}/${instanceNameRegex}"
        } returns {
            statusCode = HttpStatus.OK.value()
        }

        // Endpoint: Check readiness of instance
        operatorWireMock.get {
            urlPath like "${operatorConfig.createPath}/${instanceNameRegex}/ready"
        } returns {
            statusCode = HttpStatus.OK.value()
        }

        // Endpoint: Change log level
        operatorWireMock.post {
            urlPath like "${operatorConfig.createPath}/${instanceNameRegex}$"
        } returns {
            statusCode = HttpStatus.OK.value()
        }

        return operatorWireMock
    }

    @Bean
    @Qualifier("VZD")
    fun vzdWireMockServer(): WireMockServer {
        val vzdPort = vzdConfig.serviceUrl.split(':').last().toInt()
        val vzdWireMock = WireMockServer(WireMockConfiguration.options().port(vzdPort))

        val exampleToken = Token(access_token = "asdfasdf", token_type = "bearer", expires_in = 3600)
        val exampleDomain = Domain(domain = "example.timref.de", isInsurance = false, telematikID = "telematikId")

        // Endpoint: Authenticate at vzd
        vzdWireMock.post {
            url contains vzdConfig.tokenPath
        } returns {
            statusCode = HttpStatus.OK.value()
            body = gson.toJson(exampleToken)
        }

        // Endpoint: Authenticate at vzd
        vzdWireMock.get {
            url contains vzdConfig.authenticationPath
        } returns {
            statusCode = HttpStatus.OK.value()
            body = gson.toJson(exampleToken)
        }

        // Endpoint: Add domain to vzd
        vzdWireMock.post {
            urlPath contains vzdConfig.addDomainPath
        } returns {
            statusCode = HttpStatus.OK.value()
            body = gson.toJson(exampleDomain)
        }

        // Endpoint: Delete domain from vzd
        vzdWireMock.delete {
            url contains vzdConfig.deleteDomainPath
        } returns {
            statusCode = HttpStatus.NO_CONTENT.value()
        }

        return vzdWireMock
    }

    @Bean
    @Qualifier("Keycloak")
    fun keycloakWireMockServer(): WireMockServer {
        val keycloakAdminPort = keycloakAdminConfig.url.split(':').last().replace("/", "").toInt()
        val keycloakWireMock = WireMockServer(WireMockConfiguration.options().port(keycloakAdminPort))
        val realmNameRegex = "[a-zA-Z0-9\\-_]*"
        val uuidRegex = "[a-zA-Z0-9\\-]*"

        // Endpoint: Get OIDC token
        keycloakWireMock.post {
            urlPath equalTo "/realms/master/protocol/openid-connect/token"
        } returns {
            statusCode = HttpStatus.OK.value()
            header = "Content-Type" to "application/json"
            body = "{\"access_token\":\"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJFSkx0eEtTNW15RFJCaTdVTHdMdVpCYlRjT3RvZGNWam5Cd1RtUzQ1Z0lRIn0.eyJleHAiOjE3MDYwOTM5NjksImlhdCI6MTcwNjA5MzkwOSwianRpIjoiZWE1MTgyMzktODRjNS00NzVmLTllNTItNjlhMzI5NmM1ZTdlIiwiaXNzIjoiaHR0cDovL2hvc3QuZG9ja2VyLmludGVybmFsOjgxODAvcmVhbG1zL21hc3RlciIsImF1ZCI6WyIyOWRoOTJnczMzamR3d2Jsb2NhbGhvc3QtcmVhbG0iLCIyOWRoOTJnczMzamRtN2Fsb2NhbGhvc3QtcmVhbG0iLCIyOWRoOTJnczMzamRueGRsb2NhbGhvc3QtcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjI4Y2MwY2I3LWQ5MWYtNDk2MC1hMmMwLTJjNmY5MGRlMWI2NCIsInR5cCI6IkJlYXJlciIsImF6cCI6InJlZ2lzdHJhdGlvbi1zZXJ2aWNlIiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJjcmVhdGUtcmVhbG0iLCJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyIyOWRoOTJnczMzamR3d2Jsb2NhbGhvc3QtcmVhbG0iOnsicm9sZXMiOlsidmlldy1pZGVudGl0eS1wcm92aWRlcnMiLCJ2aWV3LXJlYWxtIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImNyZWF0ZS1jbGllbnQiLCJtYW5hZ2UtdXNlcnMiLCJxdWVyeS1yZWFsbXMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJxdWVyeS1jbGllbnRzIiwicXVlcnktdXNlcnMiLCJtYW5hZ2UtZXZlbnRzIiwibWFuYWdlLXJlYWxtIiwidmlldy1ldmVudHMiLCJ2aWV3LXVzZXJzIiwidmlldy1jbGllbnRzIiwibWFuYWdlLWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyJdfSwiMjlkaDkyZ3MzM2pkbTdhbG9jYWxob3N0LXJlYWxtIjp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sInJlZ2lzdHJhdGlvbi1zZXJ2aWNlIjp7InJvbGVzIjpbInVtYV9wcm90ZWN0aW9uIl19LCIyOWRoOTJnczMzamRueGRsb2NhbGhvc3QtcmVhbG0iOnsicm9sZXMiOlsidmlldy1pZGVudGl0eS1wcm92aWRlcnMiLCJ2aWV3LXJlYWxtIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImNyZWF0ZS1jbGllbnQiLCJtYW5hZ2UtdXNlcnMiLCJxdWVyeS1yZWFsbXMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJxdWVyeS1jbGllbnRzIiwicXVlcnktdXNlcnMiLCJtYW5hZ2UtZXZlbnRzIiwibWFuYWdlLXJlYWxtIiwidmlldy1ldmVudHMiLCJ2aWV3LXVzZXJzIiwidmlldy1jbGllbnRzIiwibWFuYWdlLWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTkyLjE2OC42NS4xIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LXJlZ2lzdHJhdGlvbi1zZXJ2aWNlIiwiY2xpZW50QWRkcmVzcyI6IjE5Mi4xNjguNjUuMSIsImNsaWVudF9pZCI6InJlZ2lzdHJhdGlvbi1zZXJ2aWNlIn0.fXF4Qd_OITJEnUG44arwEJ14dCJ6E5bt6CR3WJUEuyQQKw8dQMb9eEbkilsICBFHPg4MaKI9r6lyK23uipOfBV-TqDWs2mb-B2WJaCt7fJSPHZwwRPuN-Gzl8WBLENQlj8ZtjNTcdMSbCgagpOR-HjsjO9jU1Lc39HJ14-LyGMLxtFidCl0jGVQbthkrK7HdIwHSkTxP8Z81dOprGycZNCmbviLYFUnT4eLQKmegrzPwhC83uCKztv1cHBuLSJxQkgYuKNP0DL0eHY0RtADzNS-b5keUXjsDroR41aMq_yPkmG76A27upMw5rAnM1Ynp8o3V3GzoL3EmbK3NGsxeIg\",\"expires_in\":60,\"refresh_expires_in\":0,\"token_type\":\"Bearer\",\"not-before-policy\":0,\"scope\":\"profile email\"}"
        }

        // Endpoint: Get realm by name
        keycloakWireMock.get {
            urlPath like "/admin/realms/${realmNameRegex}"
        } returns {
            statusCode = HttpStatus.OK.value()
            header = "Content-Type" to "application/json"
            body = "{\"id\":\"1606d41b-3d97-4f2b-9e34-fed7c1ff263f\",\"realm\":\"telematik-idresultlocalhost\",\"notBefore\":0,\"defaultSignatureAlgorithm\":\"RS256\",\"revokeRefreshToken\":false,\"refreshTokenMaxReuse\":0,\"accessTokenLifespan\":300,\"accessTokenLifespanForImplicitFlow\":900,\"ssoSessionIdleTimeout\":1800,\"ssoSessionMaxLifespan\":36000,\"ssoSessionIdleTimeoutRememberMe\":0,\"ssoSessionMaxLifespanRememberMe\":0,\"offlineSessionIdleTimeout\":2592000,\"offlineSessionMaxLifespanEnabled\":false,\"offlineSessionMaxLifespan\":5184000,\"clientSessionIdleTimeout\":0,\"clientSessionMaxLifespan\":0,\"clientOfflineSessionIdleTimeout\":0,\"clientOfflineSessionMaxLifespan\":0,\"accessCodeLifespan\":60,\"accessCodeLifespanUserAction\":300,\"accessCodeLifespanLogin\":1800,\"actionTokenGeneratedByAdminLifespan\":43200,\"actionTokenGeneratedByUserLifespan\":300,\"oauth2DeviceCodeLifespan\":600,\"oauth2DevicePollingInterval\":5,\"enabled\":true,\"sslRequired\":\"external\",\"registrationAllowed\":false,\"registrationEmailAsUsername\":false,\"rememberMe\":false,\"verifyEmail\":false,\"loginWithEmailAllowed\":true,\"duplicateEmailsAllowed\":false,\"resetPasswordAllowed\":true,\"editUsernameAllowed\":false,\"bruteForceProtected\":true,\"permanentLockout\":false,\"maxFailureWaitSeconds\":900,\"minimumQuickLoginWaitSeconds\":60,\"waitIncrementSeconds\":60,\"quickLoginCheckMilliSeconds\":1000,\"maxDeltaTimeSeconds\":43200,\"failureFactor\":30,\"defaultRole\":{\"id\":\"7ae9231b-ccb0-4476-a22d-cf5f086c9ac3\",\"name\":\"default-roles-29dh92gs33jdresultlocalhost\",\"description\":\"\${role_default - roles}\",\"composite\":true,\"clientRole\":false,\"containerId\":\"1606d41b-3d97-4f2b-9e34-fed7c1ff263f\"},\"requiredCredentials\":[\"password\"],\"otpPolicyType\":\"totp\",\"otpPolicyAlgorithm\":\"HmacSHA1\",\"otpPolicyInitialCounter\":0,\"otpPolicyDigits\":6,\"otpPolicyLookAheadWindow\":1,\"otpPolicyPeriod\":30,\"otpPolicyCodeReusable\":false,\"otpSupportedApplications\":[\"totpAppFreeOTPName\",\"totpAppGoogleName\",\"totpAppMicrosoftAuthenticatorName\"],\"webAuthnPolicyRpEntityName\":\"keycloak\",\"webAuthnPolicySignatureAlgorithms\":[\"ES256\"],\"webAuthnPolicyRpId\":\"\",\"webAuthnPolicyAttestationConveyancePreference\":\"not specified\",\"webAuthnPolicyAuthenticatorAttachment\":\"not specified\",\"webAuthnPolicyRequireResidentKey\":\"not specified\",\"webAuthnPolicyUserVerificationRequirement\":\"not specified\",\"webAuthnPolicyCreateTimeout\":0,\"webAuthnPolicyAvoidSameAuthenticatorRegister\":false,\"webAuthnPolicyAcceptableAaguids\":[],\"webAuthnPolicyPasswordlessRpEntityName\":\"keycloak\",\"webAuthnPolicyPasswordlessSignatureAlgorithms\":[\"ES256\"],\"webAuthnPolicyPasswordlessRpId\":\"\",\"webAuthnPolicyPasswordlessAttestationConveyancePreference\":\"not specified\",\"webAuthnPolicyPasswordlessAuthenticatorAttachment\":\"not specified\",\"webAuthnPolicyPasswordlessRequireResidentKey\":\"not specified\",\"webAuthnPolicyPasswordlessUserVerificationRequirement\":\"not specified\",\"webAuthnPolicyPasswordlessCreateTimeout\":0,\"webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister\":false,\"webAuthnPolicyPasswordlessAcceptableAaguids\":[],\"browserSecurityHeaders\":{\"contentSecurityPolicyReportOnly\":\"\",\"xContentTypeOptions\":\"nosniff\",\"referrerPolicy\":\"no-referrer\",\"xRobotsTag\":\"none\",\"xFrameOptions\":\"SAMEORIGIN\",\"contentSecurityPolicy\":\"frame-src 'self'; frame-ancestors 'self'; object-src 'none';\",\"xXSSProtection\":\"1; mode=block\",\"strictTransportSecurity\":\"max-age=31536000; includeSubDomains\"},\"smtpServer\":{\"password\":\"**********\",\"starttls\":\"true\",\"port\":\"587\",\"auth\":\"true\",\"host\":\"smtp.office365.com\",\"from\":\"timref@akquinet.de\",\"fromDisplayName\":\"[LOKAL] TIMRef Registrierungsdienst\",\"ssl\":\"false\",\"user\":\"svc.timref@example.com\"},\"eventsEnabled\":false,\"eventsListeners\":[\"jboss-logging\"],\"enabledEventTypes\":[],\"adminEventsEnabled\":false,\"adminEventsDetailsEnabled\":false,\"identityProviders\":[],\"identityProviderMappers\":[],\"internationalizationEnabled\":true,\"supportedLocales\":[\"de\",\"en\"],\"defaultLocale\":\"de\",\"browserFlow\":\"browser\",\"registrationFlow\":\"registration\",\"directGrantFlow\":\"direct grant\",\"resetCredentialsFlow\":\"reset credentials\",\"clientAuthenticationFlow\":\"clients\",\"dockerAuthenticationFlow\":\"docker auth\",\"attributes\":{\"cibaBackchannelTokenDeliveryMode\":\"poll\",\"cibaExpiresIn\":\"120\",\"cibaAuthRequestedUserHint\":\"login_hint\",\"oauth2DeviceCodeLifespan\":\"600\",\"oauth2DevicePollingInterval\":\"5\",\"parRequestUriLifespan\":\"60\",\"cibaInterval\":\"5\",\"realmReusableOtpCode\":\"false\"},\"userManagedAccessAllowed\":false,\"clientProfiles\":{\"profiles\":[]},\"clientPolicies\":{\"policies\":[]}}"
        }

        // Endpoint: Create new Realm
        keycloakWireMock.post {
            urlPath equalTo "/admin/realms"
        } returns {
            statusCode = HttpStatus.CREATED.value()
            header = "Content-Type" to "application/json"
            header = "Location" to "http://localhost:2696/admin/realms/telematik-idresultlocalhost"
            body =
                "{\"id\":null,\"realm\":\"telematik-idresultlocalhost\",\"displayName\":null,\"displayNameHtml\":null,\"notBefore\":null,\"defaultSignatureAlgorithm\":null,\"revokeRefreshToken\":null,\"refreshTokenMaxReuse\":null,\"accessTokenLifespan\":null,\"accessTokenLifespanForImplicitFlow\":null,\"ssoSessionIdleTimeout\":null,\"ssoSessionMaxLifespan\":null,\"ssoSessionIdleTimeoutRememberMe\":null,\"ssoSessionMaxLifespanRememberMe\":null,\"offlineSessionIdleTimeout\":null,\"offlineSessionMaxLifespanEnabled\":null,\"offlineSessionMaxLifespan\":null,\"clientSessionIdleTimeout\":null,\"clientSessionMaxLifespan\":null,\"clientOfflineSessionIdleTimeout\":null,\"clientOfflineSessionMaxLifespan\":null,\"accessCodeLifespan\":null,\"accessCodeLifespanUserAction\":null,\"accessCodeLifespanLogin\":null,\"actionTokenGeneratedByAdminLifespan\":null,\"actionTokenGeneratedByUserLifespan\":null,\"oauth2DeviceCodeLifespan\":null,\"oauth2DevicePollingInterval\":null,\"enabled\":true,\"sslRequired\":null,\"passwordCredentialGrantAllowed\":null,\"registrationAllowed\":false,\"registrationEmailAsUsername\":null,\"rememberMe\":null,\"verifyEmail\":null,\"loginWithEmailAllowed\":true,\"duplicateEmailsAllowed\":null,\"resetPasswordAllowed\":true,\"editUsernameAllowed\":null,\"bruteForceProtected\":true,\"permanentLockout\":null,\"maxFailureWaitSeconds\":null,\"minimumQuickLoginWaitSeconds\":null,\"waitIncrementSeconds\":null,\"quickLoginCheckMilliSeconds\":null,\"maxDeltaTimeSeconds\":null,\"failureFactor\":null,\"privateKey\":null,\"publicKey\":null,\"certificate\":null,\"codeSecret\":null,\"roles\":null,\"groups\":null,\"defaultRoles\":null,\"defaultRole\":null,\"defaultGroups\":null,\"requiredCredentials\":null,\"passwordPolicy\":null,\"otpPolicyType\":null,\"otpPolicyAlgorithm\":null,\"otpPolicyInitialCounter\":null,\"otpPolicyDigits\":null,\"otpPolicyLookAheadWindow\":null,\"otpPolicyPeriod\":null,\"otpPolicyCodeReusable\":null,\"otpSupportedApplications\":null,\"webAuthnPolicyRpEntityName\":null,\"webAuthnPolicySignatureAlgorithms\":null,\"webAuthnPolicyRpId\":null,\"webAuthnPolicyAttestationConveyancePreference\":null,\"webAuthnPolicyAuthenticatorAttachment\":null,\"webAuthnPolicyRequireResidentKey\":null,\"webAuthnPolicyUserVerificationRequirement\":null,\"webAuthnPolicyCreateTimeout\":null,\"webAuthnPolicyAvoidSameAuthenticatorRegister\":null,\"webAuthnPolicyAcceptableAaguids\":null,\"webAuthnPolicyPasswordlessRpEntityName\":null,\"webAuthnPolicyPasswordlessSignatureAlgorithms\":null,\"webAuthnPolicyPasswordlessRpId\":null,\"webAuthnPolicyPasswordlessAttestationConveyancePreference\":null,\"webAuthnPolicyPasswordlessAuthenticatorAttachment\":null,\"webAuthnPolicyPasswordlessRequireResidentKey\":null,\"webAuthnPolicyPasswordlessUserVerificationRequirement\":null,\"webAuthnPolicyPasswordlessCreateTimeout\":null,\"webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister\":null,\"webAuthnPolicyPasswordlessAcceptableAaguids\":null,\"users\":null,\"federatedUsers\":null,\"scopeMappings\":null,\"clientScopeMappings\":null,\"clients\":[{\"id\":null,\"clientId\":\"synapse\",\"name\":\"Synapse Client\",\"description\":\"\",\"rootUrl\":\"https://29dh92gs33jdl0o.localhost\",\"adminUrl\":null,\"baseUrl\":null,\"surrogateAuthRequired\":null,\"enabled\":null,\"alwaysDisplayInConsole\":null,\"clientAuthenticatorType\":\"client-secret\",\"secret\":null,\"registrationAccessToken\":null,\"defaultRoles\":null,\"redirectUris\":[\"/_synapse/client/oidc/callback\"],\"webOrigins\":null,\"notBefore\":null,\"bearerOnly\":null,\"consentRequired\":null,\"standardFlowEnabled\":null,\"implicitFlowEnabled\":null,\"directAccessGrantsEnabled\":null,\"serviceAccountsEnabled\":null,\"authorizationServicesEnabled\":null,\"directGrantsOnly\":null,\"publicClient\":false,\"frontchannelLogout\":null,\"protocol\":null,\"attributes\":null,\"authenticationFlowBindingOverrides\":null,\"fullScopeAllowed\":null,\"nodeReRegistrationTimeout\":null,\"registeredNodes\":null,\"protocolMappers\":null,\"clientTemplate\":null,\"useTemplateConfig\":null,\"useTemplateScope\":null,\"useTemplateMappers\":null,\"defaultClientScopes\":null,\"optionalClientScopes\":null,\"authorizationSettings\":null,\"access\":null,\"origin\":null}],\"clientScopes\":null,\"defaultDefaultClientScopes\":null,\"defaultOptionalClientScopes\":null,\"browserSecurityHeaders\":null,\"smtpServer\":{\"from\":\"timref@akquinet.de\",\"fromDisplayName\":\"[LOKAL] TIMRef Registrierungsdienst\",\"host\":\"smtp.office365.com\",\"port\":\"587\",\"ssl\":\"false\",\"starttls\":\"true\",\"auth\":\"true\",\"user\":\"svc.timref@example.com\",\"password\":\"\"},\"userFederationProviders\":null,\"userFederationMappers\":null,\"loginTheme\":null,\"accountTheme\":null,\"adminTheme\":null,\"emailTheme\":null,\"eventsEnabled\":null,\"eventsExpiration\":null,\"eventsListeners\":null,\"enabledEventTypes\":null,\"adminEventsEnabled\":null,\"adminEventsDetailsEnabled\":null,\"identityProviders\":null,\"identityProviderMappers\":null,\"protocolMappers\":null,\"components\":null,\"internationalizationEnabled\":true,\"supportedLocales\":[\"de\",\"en\"],\"defaultLocale\":\"de\",\"authenticationFlows\":null,\"authenticatorConfig\":null,\"requiredActions\":[{\"alias\":\"CONFIGURE_TOTP\",\"name\":\"Configure OTP\",\"providerId\":\"CONFIGURE_TOTP\",\"enabled\":true,\"defaultAction\":true,\"priority\":10,\"config\":{}},{\"alias\":\"UPDATE_PASSWORD\",\"name\":\"Update Password\",\"providerId\":\"UPDATE_PASSWORD\",\"enabled\":true,\"defaultAction\":true,\"priority\":30,\"config\":{}},{\"alias\":\"VERIFY_EMAIL\",\"name\":\"Verify Email\",\"providerId\":\"VERIFY_EMAIL\",\"enabled\":true,\"defaultAction\":true,\"priority\":50,\"config\":{}}],\"browserFlow\":null,\"registrationFlow\":null,\"directGrantFlow\":null,\"resetCredentialsFlow\":null,\"clientAuthenticationFlow\":null,\"dockerAuthenticationFlow\":null,\"attributes\":null,\"keycloakVersion\":null,\"userManagedAccessAllowed\":null,\"social\":null,\"updateProfileOnInitialSocialLogin\":null,\"socialProviders\":null,\"applicationScopeMappings\":null,\"applications\":null,\"oauthClients\":null,\"clientTemplates\":null,\"clientProfiles\":null,\"clientPolicies\":null}"
        }

        // Endpoint: Get all client with id "synapse"
        keycloakWireMock.get {
            urlPath like "/admin/realms/${realmNameRegex}/clients"
            queryParams contains "clientId" like "synapse"
        } returns {
            statusCode = HttpStatus.OK.value()
            header = "Content-Type" to "application/json"
            body = "[{\"id\":\"7633029d-3d7c-4108-ac0f-8c606e6b0408\",\"clientId\":\"synapse\",\"name\":\"Synapse Client\",\"description\":\"\",\"rootUrl\":\"https://29dh92gs33jdresult.localhost\",\"surrogateAuthRequired\":false,\"enabled\":true,\"alwaysDisplayInConsole\":false,\"clientAuthenticatorType\":\"client-secret\",\"secret\":\"9jNRi9YvHzbV65ijZSc8RknehNJ8XtwZ\",\"redirectUris\":[\"/_synapse/client/oidc/callback\"],\"webOrigins\":[],\"notBefore\":0,\"bearerOnly\":false,\"consentRequired\":false,\"standardFlowEnabled\":true,\"implicitFlowEnabled\":false,\"directAccessGrantsEnabled\":false,\"serviceAccountsEnabled\":false,\"publicClient\":false,\"frontchannelLogout\":false,\"protocol\":\"openid-connect\",\"attributes\":{\"post.logout.redirect.uris\":\"+\",\"client.secret.creation.time\":\"1706181379\"},\"authenticationFlowBindingOverrides\":{},\"fullScopeAllowed\":true,\"nodeReRegistrationTimeout\":-1,\"defaultClientScopes\":[\"web-origins\",\"acr\",\"profile\",\"roles\",\"email\"],\"optionalClientScopes\":[\"address\",\"phone\",\"offline_access\",\"microprofile-jwt\"],\"access\":{\"view\":true,\"configure\":true,\"manage\":true}}]"
        }

        // Endpoint: Delete realm by name
        keycloakWireMock.delete {
            urlPath like "/admin/realms/${realmNameRegex}"
        } returns {
            statusCode = HttpStatus.NO_CONTENT.value()
        }

        keycloakWireMock.post {
            urlPath like "/admin/realms/${realmNameRegex}/users"
        } returns {
            statusCode = HttpStatus.CREATED.value()
            header = "Content-Type" to "application/json"
            header = "Location" to "http://localhost:2696/admin/realms/telematik-idresultlocalhost/users/d4fc1a29-d17e-43d7-9c7a-101506261607"
            body = "{\"self\":null,\"id\":null,\"origin\":null,\"createdTimestamp\":null,\"username\":\"yt7ykea1\",\"enabled\":true,\"totp\":null,\"emailVerified\":null,\"firstName\":null,\"lastName\":null,\"email\":\"test@example.de\",\"federationLink\":null,\"serviceAccountClientId\":null,\"attributes\":null,\"credentials\":[{\"id\":null,\"type\":\"password\",\"userLabel\":null,\"createdDate\":null,\"secretData\":null,\"credentialData\":null,\"priority\":null,\"value\":\"RiMvvbawHO6BAA71\",\"temporary\":false,\"device\":null,\"hashedSaltedValue\":null,\"salt\":null,\"hashIterations\":null,\"counter\":null,\"algorithm\":null,\"digits\":null,\"period\":null,\"config\":null}],\"disableableCredentialTypes\":null,\"requiredActions\":null,\"federatedIdentities\":null,\"realmRoles\":null,\"clientRoles\":null,\"clientConsents\":null,\"notBefore\":null,\"applicationRoles\":null,\"socialLinks\":null,\"groups\":null,\"access\":null,\"userProfileMetadata\":null}"
        }

        // Endpoint: Get all client with id "realm-management"
        keycloakWireMock.get {
            urlPath like "/admin/realms/${realmNameRegex}/clients"
            queryParams contains "clientId" like "realm-management"
        } returns {
            statusCode = HttpStatus.OK.value()
            header = "Content-Type" to "application/json"
            body = "[{\"id\":\"09c87f36-9230-40e7-810e-ee01bde72afb\",\"clientId\":\"realm-management\",\"name\":\"\${client_realm - management}\",\"surrogateAuthRequired\":false,\"enabled\":true,\"alwaysDisplayInConsole\":false,\"clientAuthenticatorType\":\"client-secret\",\"redirectUris\":[],\"webOrigins\":[],\"notBefore\":0,\"bearerOnly\":true,\"consentRequired\":false,\"standardFlowEnabled\":true,\"implicitFlowEnabled\":false,\"directAccessGrantsEnabled\":false,\"serviceAccountsEnabled\":false,\"publicClient\":false,\"frontchannelLogout\":false,\"protocol\":\"openid-connect\",\"attributes\":{},\"authenticationFlowBindingOverrides\":{},\"fullScopeAllowed\":false,\"nodeReRegistrationTimeout\":0,\"defaultClientScopes\":[\"web-origins\",\"acr\",\"profile\",\"roles\",\"email\"],\"optionalClientScopes\":[\"address\",\"phone\",\"offline_access\",\"microprofile-jwt\"],\"access\":{\"view\":true,\"configure\":true,\"manage\":true}}]"
        }

        // Endpoint: Get client role with name "manage-users"
        keycloakWireMock.get {
            urlPath like "/admin/realms/${realmNameRegex}/clients/${uuidRegex}/roles/manage-users"
        } returns {
            statusCode = HttpStatus.OK.value()
            header = "Content-Type" to "application/json"
            body = "{\"id\":\"d1a9791c-125d-41a4-8515-eb87713ac82e\",\"name\":\"manage-users\",\"description\":\"\${role_manage - users}\",\"composite\":false,\"clientRole\":true,\"containerId\":\"09c87f36-9230-40e7-810e-ee01bde72afb\",\"attributes\":{}}"
        }

        // Endpoint: Get client role with name "view-users"
        keycloakWireMock.get {
            urlPath like "/admin/realms/${realmNameRegex}/clients/${uuidRegex}/roles/view-users"
        } returns {
            statusCode = HttpStatus.OK.value()
            header = "Content-Type" to "application/json"
            body = "{\"id\":\"a1ebb096-0523-4511-9fec-c8dc7199b549\",\"name\":\"view-users\",\"description\":\"\${role_view - users}\",\"composite\":true,\"clientRole\":true,\"containerId\":\"09c87f36-9230-40e7-810e-ee01bde72afb\",\"attributes\":{}}"
        }

        // Endpoint: Create new role mappings for given user
        keycloakWireMock.post {
            urlPath like "/admin/realms/${realmNameRegex}/users/${uuidRegex}/role-mappings/clients/${uuidRegex}"
        } returns {
            statusCode = HttpStatus.NO_CONTENT.value()
            header = "Content-Type" to "application/json"
            body = "[{\"id\":\"d1a9791c-125d-41a4-8515-eb87713ac82e\",\"name\":\"manage-users\",\"description\":\"\${role_manage - users}\",\"scopeParamRequired\":null,\"composite\":false,\"composites\":null,\"clientRole\":true,\"containerId\":\"09c87f36-9230-40e7-810e-ee01bde72afb\",\"attributes\":{}},{\"id\":\"a1ebb096-0523-4511-9fec-c8dc7199b549\",\"name\":\"view-users\",\"description\":\"\${role_view - users}\",\"scopeParamRequired\":null,\"composite\":true,\"composites\":null,\"clientRole\":true,\"containerId\":\"09c87f36-9230-40e7-810e-ee01bde72afb\",\"attributes\":{}}]"
        }

        return keycloakWireMock
    }

    @Bean
    @Qualifier("Proxy")
    fun proxyWireMockServer(): WireMockServer {
        val messengerProxyMock = WireMockServer(WireMockConfiguration.options().port(proxyConfig.actuatorPort.toInt()))

        val logLevelRegex = "(ALL|ERROR|WARN|INFO|DEBUG|TRACE|OFF)"

        // Endpoint: Sets log level of proxy to given string
        messengerProxyMock.put {
            urlPath like "${proxyConfig.actuatorLoggingBasePath}/$logLevelRegex/.*"
        } returns {
            statusCode = HttpStatus.ACCEPTED.value()
        }

        return messengerProxyMock
    }
}
