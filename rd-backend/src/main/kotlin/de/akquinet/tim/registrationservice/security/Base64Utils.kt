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

package de.akquinet.tim.registrationservice.security

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

@JvmInline
value class Base64UrlString(val value: String)

@JvmInline
value class Base64String(val value: String)

fun String.encodeToBase64(): Base64String =
    Base64String(Base64.getEncoder().encodeToString(toByteArray()))

fun String.encodeToBase64Url(): Base64UrlString =
    Base64UrlString(Base64.getUrlEncoder().encodeToString(toByteArray()))

fun Base64String.decodeToByteArray(): ByteArray = Base64.getDecoder().decode(value)

fun Base64UrlString.decodeToString() = Base64.getUrlDecoder().decode(value).let { String(it) }

fun Base64String.decodeToString() = Base64.getDecoder().decode(value).let { String(it) }

fun Base64String.toPublicEcKey(): PublicKey =
    decodeToByteArray().let { bytes ->
        val keySpec = X509EncodedKeySpec(bytes)
        val keyFactory = KeyFactory.getInstance("EC")
        keyFactory.generatePublic(keySpec)
    }

fun Base64String.toPrivateEcKey(): PrivateKey =
    decodeToByteArray().let { bytes ->
        val keySpec = PKCS8EncodedKeySpec(bytes)
        val keyFactory = KeyFactory.getInstance("EC")
        keyFactory.generatePrivate(keySpec)
    }

fun Base64String.toX509Certificate(): X509Certificate =
    decodeToByteArray().inputStream().use {
        CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
    }

@JvmInline
value class PemString(val value: String)

fun PemString.toX509Certificate(): X509Certificate =
    value.toByteArray().inputStream().use {
        CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
    }

fun PemString.toBase64String(): Base64String =
    Base64String(value.replace(Regex("-{5}.+?-{5}"), ""))
