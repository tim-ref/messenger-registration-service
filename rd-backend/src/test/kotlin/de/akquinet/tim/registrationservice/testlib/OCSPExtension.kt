/*
 * Copyright (C) 2025 akquinet GmbH
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

package de.akquinet.tim.registrationservice.testlib

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.core.extensions.SpecExtension
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers
import org.bouncycastle.asn1.isismtt.ocsp.CertHash
import org.bouncycastle.asn1.ocsp.CertID
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.Extensions
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.ocsp.*
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.URI
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.RSAPrivateCrtKey
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

private class OCSPServerContextElement(val ocspServer: OCSPServer) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<OCSPServerContextElement>
}

private fun ByteArray.hexString() = joinToString("") { "%02x".format(it) }

private val CertificateID.pretty: String
    get() = "CertificateID(${hashAlgOID.id}, ${issuerNameHash.hexString()}, ${issuerKeyHash.hexString()}, ${
        serialNumber.toString(
            16
        )
    })"

private val digestCalculatorProvider = JcaDigestCalculatorProviderBuilder().build()
private val digestAlgorithmIdentifierFinder = DefaultDigestAlgorithmIdentifierFinder()

private fun makeDigestCalculator(digAlgName: String): DigestCalculator =
    digestCalculatorProvider.get(digestAlgorithmIdentifierFinder.find(digAlgName))

private fun PrivateKey.makeSigner() = when (this) {
    is ECPrivateKey -> {
        JcaContentSignerBuilder("SHA256withECDSA")
    }

    is RSAPrivateCrtKey -> {
        JcaContentSignerBuilder("SHA256withRSA")
    }

    else -> throw IllegalArgumentException("Unsupported key type: $javaClass")
}.build(this)

// ProvOcspRevocationChecker constructs the AlgorithmIdentifier for SHA-1 differently (incorrectly?): The params are (Java) null instead
// of DERNull.INSTANCE, leading to a different ASN.1 structure and non-matching CertificateIDs.
// This function treats DERNull.INSTANCE and null as equal.
private fun AlgorithmIdentifier.matches(other: AlgorithmIdentifier): Boolean =
    other.algorithm == algorithm && (other.parameters == parameters || (other.parameters == null && parameters == DERNull.INSTANCE) || (other.parameters == DERNull.INSTANCE && parameters == null))

private fun CertID.matches(other: CertID): Boolean =
    other.hashAlgorithm.matches(hashAlgorithm) && other.issuerNameHash == issuerNameHash && other.issuerKeyHash == issuerKeyHash && other.serialNumber == serialNumber

private fun CertificateID.matches(other: CertificateID): Boolean = other.toASN1Primitive().matches(toASN1Primitive())

private class OCSPServer(private val debug: Boolean = false) : AutoCloseable {

    private data class OCSPServerException(override val message: String) : Exception(message)

    private val log = LoggerFactory.getLogger(OCSPExtension::class.java)

    private data class TrainedResponse(
        val signerCert: X509CertificateHolder,
        val signer: ContentSigner,
        val certID: CertificateID,
        val certStatus: CertificateStatus?,
        val thisUpdate: Instant,
        val nextUpdate: Instant,
        val extensions: Extensions
    )

    private val responses: MutableMap<CertificateID, TrainedResponse> = mutableMapOf()

    fun addResponse(
        signerCert: X509CertificateHolder,
        signer: ContentSigner,
        certID: CertificateID,
        certStatus: CertificateStatus?,
        thisUpdate: Instant,
        nextUpdate: Instant,
        extensions: Extensions
    ) {
        if (responses.containsKey(certID)) {
            throw OCSPServerException("$certID already exists")
        }
        responses[certID] = TrainedResponse(
            signerCert, signer, certID, certStatus, thisUpdate, nextUpdate, extensions
        )
    }

    fun reset() {
        responses.clear()
    }


    private fun respond(request: OCSPReq): OCSPResp {
        val matchingResponses = request.requestList.map { req ->
            val matches = responses.filterKeys { it.matches(req.certID) }.values
            if (matches.size != 1) {
                throw OCSPServerException("Expected exactly 1 match for ${req.certID.pretty}, got ${matches.size}")
            }
            matches.single()
        }
        val certs = matchingResponses.map { it.signerCert }.toSet()
        if (certs.size != 1) {
            throw OCSPServerException("Found ${certs.size} different OCSP signer certs - a response can only be signed by a single one")
        }
        val cert = certs.single()
        val signers = matchingResponses.map { it.signer }.toSet()
        if (signers.size != 1) {
            throw OCSPServerException("Found ${signers.size} different OCSP signers - a response can only be signed by a single one")
        }
        val signer = signers.single()


        return OCSPRespBuilder().build(
            OCSPRespBuilder.SUCCESSFUL, BasicOCSPRespBuilder(
                cert.subjectPublicKeyInfo, makeDigestCalculator("SHA-1"),
            ).also { builder ->
                matchingResponses.forEach { response ->
                    builder.addResponse(
                        response.certID,
                        response.certStatus,
                        Date.from(response.thisUpdate),
                        Date.from(response.nextUpdate),
                        response.extensions
                    )
                }
            }.build(signer, arrayOf(cert), Date())
        )
    }

    // GET: the OCSPReq is the decoded last path segment
    fun handleGet(exchange: HttpExchange) {

        val req = OCSPReq(Base64.getDecoder().decode(exchange.requestURI.path.removePrefix(exchange.httpContext.path)))
        val resp = respond(req).encoded

        exchange.sendResponseHeaders(200, resp.size.toLong())
        exchange.responseBody.use {
            it.write(resp)
        }
    }

    fun handlePost(exchange: HttpExchange) {
        val resp = respond(OCSPReq(exchange.requestBody.readAllBytes())).encoded

        exchange.sendResponseHeaders(200, resp.size.toLong())
        exchange.responseBody.use {
            it.write(resp)
        }
    }


    val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0).also {
        it.createContext("/") { exchange ->
            try {
                when (exchange.requestMethod.uppercase()) {
                    "GET" -> handleGet(exchange)
                    "POST" -> handlePost(exchange)
                    else -> {
                        exchange.sendResponseHeaders(405, 0)
                        exchange.close()
                    }

                }
            } catch (ex: Throwable) {
                if (debug) {
                    log.error("Error handling request in mock OCSP server", ex)
                } else {
                    log.warn("Error handling request in mock OCSP server: ${ex.message} (use extension(OCSPExtension(debug=true)) for stack trace)")
                }
                exchange.sendResponseHeaders(500, 0)
            }
        }
        it.start()
    }

    override fun close() {
        server.stop(0)
    }
}


class OCSPExtension(
    debug: Boolean = false
) : SpecExtension, TestCaseExtension {
    private val server = lazy {
        OCSPServer(debug)
    }

    override suspend fun intercept(spec: Spec, execute: suspend (Spec) -> Unit) {
        server.value.use {
            withContext(OCSPServerContextElement(it)) {
                execute(spec)
            }
        }
    }

    override suspend fun intercept(testCase: TestCase, execute: suspend (TestCase) -> TestResult): TestResult {
        if (testCase.type == TestType.Test) {
            val result = execute(testCase)
            getContextElement().ocspServer.reset()
            return result
        } else {
            return execute(testCase)
        }
    }
}

private suspend fun getContextElement(): OCSPServerContextElement {
    val ctx = coroutineContext[OCSPServerContextElement]
        ?: error("no OCSPServer defined in this coroutine context. Is OCSPExtension installed?")
    return ctx
}

internal suspend fun mockOcspResponderURI(): URI = getContextElement().ocspServer.server.address.let {
    URI("http", null, it.hostString, it.port, "/", null, null)
}

internal suspend fun withOcspResponse(
    signerCert: X509Certificate,
    signer: PrivateKey,
    issuerCert: X509Certificate,
    subjectCert: X509Certificate,
    // example: RevokedStatus(Date(), CRLReason.keyCompromise)
    // note: CertificateStatus.GOOD is null, so status needs to be nullable :-(
    status: CertificateStatus? = CertificateStatus.GOOD,
    thisUpdate: Instant = Instant.now(),
    nextUpdate: Instant = thisUpdate.plus(Duration.ofHours(1)),
) {
    val certId = CertificateID(
        makeDigestCalculator("SHA-1"),
        X509CertificateHolder(issuerCert.encoded),
        subjectCert.serialNumber
    )
    val certHash = makeDigestCalculator("SHA-256").let {
        it.outputStream.use { o -> o.write(subjectCert.encoded) }
        Extension(
            ISISMTTObjectIdentifiers.id_isismtt_at_certHash,
            false,
            CertHash(it.algorithmIdentifier, it.digest).encoded,
        )
    }

    getContextElement().ocspServer.addResponse(
        X509CertificateHolder(signerCert.encoded),
        signer.makeSigner(),
        certId,
        status,
        thisUpdate,
        nextUpdate,
        Extensions(certHash)
    )
}
