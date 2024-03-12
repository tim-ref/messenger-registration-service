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

package de.akquinet.timref.registrationservice.unitTests.api.openid

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.util.Base64URL
import de.akquinet.timref.registrationservice.service.openid.RegServiceOpenIdTokenCreatorService
import de.akquinet.timref.registrationservice.config.KeyConfig
import de.akquinet.timref.registrationservice.config.TokenConfig
import de.akquinet.timref.registrationservice.config.VZDConfig
import de.akquinet.timref.registrationservice.persistance.orgAdmin.model.OrgAdminEntity
import de.akquinet.timref.registrationservice.security.signature.SignatureServiceImpl
import de.akquinet.timref.registrationservice.security.signature.jose4extension.Brainpool
import de.akquinet.timref.registrationservice.service.openid.trimPemString
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.security.Security
import java.time.Instant
import java.util.*

class TestSignatureServiceImpl : SignatureServiceImpl(
    logger = LoggerFactory.getLogger(TestSignatureServiceImpl::class.java),
    vzdConfig = VZDConfig(
        serviceUrl = "url",
        tokenUrl = "url",
        tokenPath = "path",
        authenticationPath = "authPath",
        healthPath = "healthPath",
        federationListPath = "fedPath",
        federationCheckPath = "checkPath",
        userWhereIsPath = "wherePath",
        checkRevocationStatus = false,
        addDomainPath = "domainPath",
        deleteDomainPath = "deletePath",
        clientId = "clientId",
        clientSecret = "clientSecret",
        trustStorePath = "path/to/secret",
        trustStorePassword = "trustStorePassword"
    ),
)

private val keyConfig = KeyConfig(
    privateKey = """-----BEGIN PRIVATE KEY-----
        |MIGIAgEAMBQGByqGSM49AgEGCSskAwMCCAEBBwRtMGsCAQEEIKLkl5iqMgeHB4MS
        |DEY+6YMezDW1/gKAHHMZLFdo71GHoUQDQgAEANy7YN285VwAqybcUGu18bSodYpi
        |zO0rrzLopEnPF26GpxpfyVQDRDG8jOpu3YwBvv2Vkp3HmnpSe5hnPXzbXA==
        |-----END PRIVATE KEY-----""".trimMargin(),
    certificate = """-----BEGIN CERTIFICATE-----
        |MIIBczCCARkCAQEwCgYIKoZIzj0EAwIwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgM
        |ClNvbWUtU3RhdGUxITAfBgNVBAoMGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAe
        |Fw0yMzEwMjcxMzAwMzVaFw0yODEwMjUxMzAwMzVaMEUxCzAJBgNVBAYTAkFVMRMw
        |EQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0
        |eSBMdGQwWjAUBgcqhkjOPQIBBgkrJAMDAggBAQcDQgAEANy7YN285VwAqybcUGu1
        |8bSodYpizO0rrzLopEnPF26GpxpfyVQDRDG8jOpu3YwBvv2Vkp3HmnpSe5hnPXzb
        |XDAKBggqhkjOPQQDAgNIADBFAiEAm9ItFTHehdo6404tZX9hEwsvTD/XJTo/6oex
        |03bp654CIFYtTvbqGgftoJVmvIzdxBSaD3yRn2Y7IS2Re2Lp9xY2
        |-----END CERTIFICATE-----""".trimMargin(),
    caCertificate = """-----BEGIN CERTIFICATE-----
        |MIIB3zCCAYagAwIBAgIUWqiEQPTL4/ZU6WatC3n5cXLdmi4wCgYIKoZIzj0EAwIw
        |RTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoMGElu
        |dGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMzEwMjcxMjU3MTlaFw0yODEwMjUx
        |MjU3MTlaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYD
        |VQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwWjAUBgcqhkjOPQIBBgkrJAMD
        |AggBAQcDQgAEd5yB3Tf/PkG3rj95Bzqnt+VmPE1VqVfJTwYEg9WoAkZHta2zm/rI
        |Qc3yqCWgFeFvexDMYQ0Lb+WATsVkUYb7gqNTMFEwHQYDVR0OBBYEFGHgVM/2g0vC
        |sOgvsD3kz8Q54dFcMB8GA1UdIwQYMBaAFGHgVM/2g0vCsOgvsD3kz8Q54dFcMA8G
        |A1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDRwAwRAIgXK8LVRnLhiUMiwvdI3Tz
        |/woZaav3eE6Xc4C1kOvS7EsCIF159howGAnuUHvqbgSy8vyGhE9nYeP3B8UHPDpK
        |lTEb
        |-----END CERTIFICATE-----""".trimMargin()
)

private val tokenConfig = TokenConfig(
    issuer = "issuer",
    audience = "aud1/owner-authenticate",
    validitySeconds = 3600
)

private val defaultAdmin = OrgAdminEntity(
    id = UUID.randomUUID(),
    telematikId = "akq-123",
    mxId = "@OrgAdmin:tim.akquinet.de",
    professionOid = "1.2.3.4",
    serverName = "akq"
)

class RegServiceOpenIdTokenCreatorTest : DescribeSpec({
    // BC providers required for certificates and TLS cipher suites using brainpool curves
    Security.insertProviderAt(BouncyCastleProvider(), 1)
    Brainpool.installExtension()

    describe("RegServiceOpenIdTokenCreatorTest") {
        val signatureService = TestSignatureServiceImpl()

        fun parseToken(token: String): JWSObject {
            val parts = token.split(".")
            parts shouldHaveSize 3
            return JWSObject(Base64URL(parts[0]), Base64URL(parts[1]), Base64URL(parts[2]))
        }

        fun createToken(orgAdmin: OrgAdminEntity = defaultAdmin): String =
            RegServiceOpenIdTokenCreatorService(
                logger = LoggerFactory.getLogger(RegServiceOpenIdTokenCreatorService::class.java),
                keyConfig = keyConfig,
                tokenConfig = tokenConfig,
                signatureService
            ).createToken(orgAdmin)

        it("can create a token that has correct sub") {
            val token = createToken(defaultAdmin)
            val tokenObject = parseToken(token)

            tokenObject.payload.toJSONObject()["sub"] shouldBe defaultAdmin.mxId
        }

        it("creates token with correct issuer") {
            val token = createToken(defaultAdmin)
            val tokenObject = parseToken(token)

            tokenObject.payload.toJSONObject()["iss"] shouldBe tokenConfig.issuer
        }

        it("creates token with correct aud") {
            val token = createToken(defaultAdmin)
            val tokenObject = parseToken(token)

            tokenObject.payload.toJSONObject()["aud"] shouldBe tokenConfig.audience
        }

        it("creates token with correct exp") {
            val token = createToken(defaultAdmin)
            val tokenObject = parseToken(token)

            val exp = tokenObject.payload.toJSONObject()["exp"] as Long
            val iat = tokenObject.payload.toJSONObject()["iat"] as Long
            exp shouldBe iat + tokenConfig.validitySeconds
        }

        it("creates token with correct iat") {
            val before = Instant.now().epochSecond
            val token = createToken(defaultAdmin)
            val after = Instant.now().epochSecond

            val tokenObject = parseToken(token)

            val iat = tokenObject.payload.toJSONObject()["iat"] as Long
            iat shouldBeInRange before..after
        }

        it("test pem string trimming") {
            val expectedTrimResult =
                ("MIGIAgEAMBQGByqGSM49AgEGCSskAwMCCAEBBwRtMGsCAQEEIKLkl5iqMgeHB4MSDEY+6YMezDW1/gK" +
                        "AHHMZLFdo71GHoUQDQgAEANy7YN285VwAqybcUGu18bSodYpizO0rrzLopEnPF26GpxpfyVQD" +
                        "RDG8jOpu3YwBvv2Vkp3HmnpSe5hnPXzbXA==").trimMargin()
            keyConfig.privKey.trimPemString() shouldBe expectedTrimResult
        }
    }
})
