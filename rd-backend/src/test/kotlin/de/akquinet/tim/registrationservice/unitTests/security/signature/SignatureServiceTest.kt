/*
 * Copyright (C) 2023 - 2025 akquinet GmbH
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

package de.akquinet.tim.registrationservice.unitTests.security.signature

import de.akquinet.tim.registrationservice.config.VZDConfig
import de.akquinet.tim.registrationservice.config.removeWhiteSpace
import de.akquinet.tim.registrationservice.security.PemString
import de.akquinet.tim.registrationservice.security.signature.CertPathValidationResult
import de.akquinet.tim.registrationservice.security.signature.JwsSignatureVerificationResult
import de.akquinet.tim.registrationservice.security.signature.SignatureServiceImpl
import de.akquinet.tim.registrationservice.security.signature.jose4extension.Brainpool
import de.akquinet.tim.registrationservice.security.toBase64String
import de.akquinet.tim.registrationservice.security.toPrivateEcKey
import de.akquinet.tim.registrationservice.security.toX509Certificate
import de.akquinet.tim.registrationservice.testlib.OCSPExtension
import de.akquinet.tim.registrationservice.testlib.mockOcspResponderURI
import de.akquinet.tim.registrationservice.testlib.withOcspResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bouncycastle.asn1.x509.CRLReason
import org.bouncycastle.cert.ocsp.RevokedStatus
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.security.Security
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.util.*

private class TestSignatureServiceImpl(
    val useTrustAnchors: Set<TrustAnchor> = setOf(),
    checkRevocationStatus: Boolean = false,
    ocspResponder: URI? = null,
) : SignatureServiceImpl(
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
        checkRevocationStatus = checkRevocationStatus,
        ocspResponder = ocspResponder,
        addDomainPath = "domainPath",
        deleteDomainPath = "deletePath",
        clientId = "clientId",
        clientSecret = "clientSecret",
        trustStorePath = "path/to/secret",
        trustStorePassword = "trustStorePassword"
    ),
) {
    override fun getTrustAnchors() = useTrustAnchors
}

class SignatureServiceTest : DescribeSpec({

    extension(OCSPExtension())

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
                .verifyCertificatePath(certs)
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
                .verifyCertificatePath(incompleteChain)
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
                .verifyCertificatePath(listOf(x509SigningCert))
                .shouldBeInstanceOf<CertPathValidationResult.Valid>()
        }

        describe("OCSP checks") {

            val caCert = PemString(getResourceText("brainpool/ca.crt")).toX509Certificate()
            val caOcspKey =
                PemString(getResourceText("brainpool/priv-key.ocsp-signer-ca.pkcs8.pem").removeWhiteSpace())
                    .toBase64String()
                    .toPrivateEcKey()
            val caOcspCert = PemString(getResourceText("brainpool/ocsp-signer-ca.crt")).toX509Certificate()

            val intermediateCert =
                PemString(getResourceText("brainpool/intermediate.crt")).toX509Certificate()
            val intermediateOcspKey =
                PemString(getResourceText("brainpool/priv-key.ocsp-signer-intermediate.pkcs8.pem").removeWhiteSpace())
                    .toBase64String()
                    .toPrivateEcKey()
            val intermediateOcspCert =
                PemString(getResourceText("brainpool/ocsp-signer-intermediate.crt")).toX509Certificate()

            val serverCert = PemString(getResourceText("brainpool/server.crt")).toX509Certificate()
/* TODO: fixme

            it("work") {
                withOcspResponse(
                    signerCert = caOcspCert,
                    signer = caOcspKey,
                    issuerCert = caCert,
                    subjectCert = intermediateCert
                )
                withOcspResponse(
                    signerCert = intermediateOcspCert,
                    signer = intermediateOcspKey,
                    issuerCert = intermediateCert,
                    subjectCert = serverCert
                )

                val signatureService =
                    TestSignatureServiceImpl(
                        useTrustAnchors = setOf(TrustAnchor(caCert, null)),
                        checkRevocationStatus = true,
                        ocspResponder = mockOcspResponderURI(),
                    )
                val result =
                    signatureService.verifyCertificatePath(listOf(serverCert, intermediateCert))
                result.shouldBeInstanceOf<CertPathValidationResult.Valid>()
            }

            it("disallow revoked certificates") {
                withOcspResponse(
                    signerCert = caOcspCert,
                    signer = caOcspKey,
                    issuerCert = caCert,
                    subjectCert = intermediateCert
                )
                withOcspResponse(
                    signerCert = intermediateOcspCert,
                    signer = intermediateOcspKey,
                    issuerCert = intermediateCert,
                    subjectCert = serverCert,
                    status = RevokedStatus(Date(), CRLReason.keyCompromise),
                )

                val signatureService =
                    TestSignatureServiceImpl(
                        useTrustAnchors = setOf(TrustAnchor(caCert, null)),
                        checkRevocationStatus = true,
                        ocspResponder = mockOcspResponderURI(),
                    )
                val result =
                    signatureService.verifyCertificatePath(listOf(serverCert, intermediateCert))
                result
                    .shouldBeInstanceOf<CertPathValidationResult.Invalid>()
                    .exception.cause.shouldNotBeNull()
                    .message.shouldContain("Certificate has been revoked, reason: KEY_COMPROMISE")
            }
*/

            it("fail on missing response") {
                // no withOcspResponse at all
                val signatureService =
                    TestSignatureServiceImpl(
                        useTrustAnchors = setOf(TrustAnchor(caCert, null)),
                        checkRevocationStatus = true,
                        ocspResponder = mockOcspResponderURI(),
                    )
                val result =
                    signatureService.verifyCertificatePath(listOf(serverCert, intermediateCert))
                // Due to the setup with the http server in OCSPExtension, it's not trivial to get the error message here
                result.shouldBeInstanceOf<CertPathValidationResult.Invalid>()
            }

            it("fail on missing response for ca") {
                withOcspResponse(
                    signerCert = intermediateOcspCert,
                    signer = intermediateOcspKey,
                    issuerCert = intermediateCert,
                    subjectCert = serverCert
                )

                val signatureService =
                    TestSignatureServiceImpl(
                        useTrustAnchors = setOf(TrustAnchor(caCert, null)),
                        checkRevocationStatus = true,
                        ocspResponder = mockOcspResponderURI(),
                    )
                val result =
                    signatureService.verifyCertificatePath(listOf(serverCert, intermediateCert))
                result
                    .shouldBeInstanceOf<CertPathValidationResult.Invalid>()
            }
        }
    }
})
