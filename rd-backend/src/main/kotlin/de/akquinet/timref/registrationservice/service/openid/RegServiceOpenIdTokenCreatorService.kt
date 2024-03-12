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

package de.akquinet.timref.registrationservice.service.openid

import de.akquinet.timref.registrationservice.config.KeyConfig
import de.akquinet.timref.registrationservice.config.TokenConfig
import de.akquinet.timref.registrationservice.persistance.orgAdmin.model.OrgAdminEntity
import de.akquinet.timref.registrationservice.security.PemString
import de.akquinet.timref.registrationservice.security.signature.SignatureService
import de.akquinet.timref.registrationservice.security.toBase64String
import de.akquinet.timref.registrationservice.security.toPrivateEcKey
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.security.PrivateKey
import java.time.ZoneId
import java.time.ZonedDateTime


@Service
class RegServiceOpenIdTokenCreatorService(
    private val logger: Logger,
    private val keyConfig: KeyConfig,
    private val tokenConfig: TokenConfig,
    private val signatureService: SignatureService,
) {

    private val key: PrivateKey = createECKeyFromConfig(keyConfig)

    private fun createECKeyFromConfig(config: KeyConfig): PrivateKey =
        PemString(config.privKey)
            .toBase64String()
            .toPrivateEcKey()

    fun createToken(orgAdmin: OrgAdminEntity): String {
        val issuedAtTime = ZonedDateTime.now(ZoneId.of("UTC")).toInstant()
        val expiryTime = issuedAtTime.plusSeconds(tokenConfig.validitySeconds)
        logger.debug("Tokenconfig: Aud: ${tokenConfig.audience}, iss: ${tokenConfig.issuer}")
        val payload = """{
                "sub": "${orgAdmin.mxId}",
                "idNummer": "${orgAdmin.telematikId}",
                "professionOID": "${orgAdmin.professionOid}",
                "aud": "${tokenConfig.audience}",
                "iss": "${tokenConfig.issuer}",
                "iat": ${issuedAtTime.epochSecond},
                "exp": ${expiryTime.epochSecond}    
        }""".trimIndent()

        val headers = listOf(
            Pair("alg","BP256R1"),
            Pair("typ","JWT"),
            Pair("x5c", arrayOf(keyConfig.cert.trimPemString(), keyConfig.caCert.trimPemString()))
        )

        // this signs the jws
        return signatureService.createJwsString(payload, headers, key)
    }
}

fun String.trimPemString(): String =
    replace(Regex("-{5}.+?-{5}"), "")
