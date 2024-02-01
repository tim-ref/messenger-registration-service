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

package de.akquinet.timref.registrationservice.unitTests.security.signature

import de.akquinet.timref.registrationservice.config.VZDConfig
import de.akquinet.timref.registrationservice.config.removeWhiteSpace
import de.akquinet.timref.registrationservice.security.PemString
import de.akquinet.timref.registrationservice.security.signature.CertPathValidationResult
import de.akquinet.timref.registrationservice.security.signature.JwsSignatureVerificationResult
import de.akquinet.timref.registrationservice.security.signature.SignatureServiceImpl
import de.akquinet.timref.registrationservice.security.signature.jose4extension.Brainpool
import de.akquinet.timref.registrationservice.security.toBase64String
import de.akquinet.timref.registrationservice.security.toPrivateEcKey
import de.akquinet.timref.registrationservice.security.toX509Certificate
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

private class TestSignatureServiceImpl(
    val useTrustAnchors: Set<TrustAnchor> = setOf()
) : SignatureServiceImpl(
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
) {
    override fun getTrustAnchors() = useTrustAnchors
    override fun buildPkixParameters(checkRevocationStatus: Boolean) = PKIXParameters(getTrustAnchors()).also {
        it.isRevocationEnabled = checkRevocationStatus
    }
}

class SignatureServiceTest : DescribeSpec({
    fun getResourceText(name: String): String =
        javaClass.classLoader.getResource("testCerts/$name")!!.readText()

    describe("SignatureServiceTest") {
        // BC providers required for certificates and TLS cipher suites using brainpool curves and install custom algorithm
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        Brainpool.installExtension()

        it("signs a payload and verifies the signature") {
            val signatureService = TestSignatureServiceImpl()

            val privateEcKey = PemString(getResourceText("brainpool/priv-key.intermediate.pkcs8.pem").removeWhiteSpace())
                .toBase64String()
                .toPrivateEcKey()
            val x509Certificate = PemString(getResourceText("brainpool/intermediate.crt"))
                .toX509Certificate()

            val payload = "{\"text\": \"Hi\"}"
            val headers = listOf(
                Pair("alg","BP256R1"),
                Pair("typ","JWT")
            )

            val signedJwsString = signatureService.createJwsString(
                payload = payload,
                headers = headers,
                privateKey = privateEcKey
            )

            val jwsVerificationResult = signatureService.parseAndVerifySignature(
                jwsRaw = signedJwsString,
                publicKey = x509Certificate.publicKey
            ).shouldBeInstanceOf<JwsSignatureVerificationResult.Valid>()

            val jws = jwsVerificationResult.jws
            jws.payload shouldBe payload
        }

        it("verifies a certificate path") {
            val certs: List<X509Certificate> = listOf(
                PemString(getResourceText("brainpool/server.crt"))
                    .toX509Certificate(),
                PemString(getResourceText("brainpool/intermediate.crt"))
                    .toX509Certificate(),
                PemString(getResourceText("brainpool/ca.crt"))
                    .toX509Certificate()
            )

            val trustAnchors = setOf(TrustAnchor(certs.last(), null))

            val signatureService = TestSignatureServiceImpl(
                useTrustAnchors = trustAnchors
            )

            signatureService
                .verifyCertificatePath(certs, false)
                .shouldBeInstanceOf<CertPathValidationResult.Valid>()
        }

        it("does not verify certificate path if path does not chain with trust anchor") {
            val certs: List<X509Certificate> = listOf(
                PemString(getResourceText("brainpool/server.crt"))
                    .toX509Certificate(),
                PemString(getResourceText("brainpool/intermediate.crt"))
                    .toX509Certificate(),
                PemString(getResourceText("brainpool/ca.crt"))
                    .toX509Certificate()
            )

            val trustAnchors = setOf(TrustAnchor(certs.last(), null))

            val signatureService = TestSignatureServiceImpl(
                useTrustAnchors = trustAnchors
            )

            val incompleteChain = certs.take(1) // does not chain with trust anchor
            signatureService
                .verifyCertificatePath(incompleteChain, false)
                .shouldBeInstanceOf<CertPathValidationResult.Invalid>()
        }

        it("verifies certificate path and signature") {
            val signingKey = PemString(getResourceText("brainpool/priv-key.intermediate.pkcs8.pem").removeWhiteSpace())
                .toBase64String()
                .toPrivateEcKey()

            val x509RootCert = PemString(getResourceText("brainpool/ca.crt"))
                .toX509Certificate()
            val x509SigningCert = PemString(getResourceText("brainpool/intermediate.crt"))
                .toX509Certificate()

            val trustAnchor = TrustAnchor(x509RootCert, null)

            val signatureService = TestSignatureServiceImpl(
                useTrustAnchors = setOf(trustAnchor)
            )

            val payload = "{\"text\": \"Hi\"}"
            val headers = listOf(
                Pair("alg","BP256R1"),
                Pair("typ","JWT")
            )

            val signedJwsString = signatureService.createJwsString(
                payload = payload,
                headers = headers,
                privateKey = signingKey
            )

            signatureService.parseAndVerifySignature(
                jwsRaw = signedJwsString,
                publicKey = x509SigningCert.publicKey
            ).shouldBeInstanceOf<JwsSignatureVerificationResult.Valid>()

            signatureService
                .verifyCertificatePath(listOf(x509SigningCert), false)
                .shouldBeInstanceOf<CertPathValidationResult.Valid>()
        }
    }
})
