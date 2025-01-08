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

package de.akquinet.tim.registrationservice.security.signature

import de.akquinet.tim.registrationservice.config.VZDConfig
import de.akquinet.tim.registrationservice.openapi.model.federation.FederationList
import de.akquinet.tim.registrationservice.security.signature.jose4extension.BP256R1Algorithm
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.consumer.InvalidJwtException
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.*


sealed interface JwsVerificationResult {
    data class Valid(val federationList: FederationList) : JwsVerificationResult
    object Invalid : JwsVerificationResult
}

sealed interface JwsSignatureVerificationResult {
    data class Valid(val jws: JsonWebSignature) : JwsSignatureVerificationResult
    data class Invalid(val message: String?) : JwsSignatureVerificationResult
}

sealed interface CertPathValidationResult {
    object Valid : CertPathValidationResult
    data class Invalid(
        val exception: GeneralSecurityException,
        val message: String
    ) : CertPathValidationResult
}

@Service
class SignatureServiceImpl(
    private val logger: Logger,
    val vzdConfig: VZDConfig,
) : SignatureService {

    fun getTrustAnchors(): Set<TrustAnchor> {
        val keyStore = loadTrustStore()
        val params = PKIXParameters(keyStore)

        return params.trustAnchors
    }

    private fun loadTrustStore(): KeyStore? {
        val fileInputStream = FileInputStream(vzdConfig.trustStorePath)
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        trustStore.load(fileInputStream, vzdConfig.trustStorePassword.toCharArray())
        return trustStore
    }

    fun buildPkixParameters(checkRevocationStatus: Boolean = false): PKIXParameters = PKIXParameters(getTrustAnchors()).also {
        // TODO: gematik would like to provide an OCSP responder at a later date, in which case this parameter should be replaced
        it.isRevocationEnabled = checkRevocationStatus
    }

    override fun createJwsString(payload: String, headers: List<Pair<String, Any>>, privateKey: PrivateKey): String {
        val jws = JsonWebSignature()

        jws.setAlgorithmConstraints(
            AlgorithmConstraints(
                AlgorithmConstraints.ConstraintType.PERMIT,
                BP256R1Algorithm.ALGORITHM
            )
        )

        jws.payload = payload
        headers.forEach { jws.setHeader(it.first, it.second) }
        jws.key = privateKey

        return jws.compactSerialization
    }

    override fun parseAndVerifySignature(jwsRaw: String, publicKey: PublicKey): JwsSignatureVerificationResult {
        val jws = JsonWebSignature()

        jws.setAlgorithmConstraints(
            AlgorithmConstraints(
                AlgorithmConstraints.ConstraintType.PERMIT,
                BP256R1Algorithm.ALGORITHM
            )
        )

        jws.compactSerialization = jwsRaw
        jws.key = publicKey

        return try {
            if(jws.verifySignature()){
                JwsSignatureVerificationResult.Valid(jws)
            }
            else {
                JwsSignatureVerificationResult.Invalid("Verification returned invalid")
            }

        } catch (e: InvalidJwtException) {
            logger.error("Exception during parsing: ", e)
            JwsSignatureVerificationResult.Invalid("Error during signature verification: ${e.message}")
        }
    }

    override fun verifyCertificatePath(
        certs: List<X509Certificate>,
        checkRevocationStatus: Boolean
    ): CertPathValidationResult = try {
        // Verification fails if the rootCA cert is included in the verification path.
        val pkixParameters = buildPkixParameters(checkRevocationStatus)

        val certPathValidator = CertPathValidator.getInstance("PKIX")
        val certFactory = CertificateFactory.getInstance("X509")
        val certPath = certFactory.generateCertPath(certs)

        certPathValidator.validate(certPath, pkixParameters)
        CertPathValidationResult.Valid
    } catch (e: CertificateException) {
        CertPathValidationResult.Invalid(e, "Could not generate certification path")
    } catch (e: CertPathValidatorException) {
        CertPathValidationResult.Invalid(e, "Could not verify certification path")
    }
}
