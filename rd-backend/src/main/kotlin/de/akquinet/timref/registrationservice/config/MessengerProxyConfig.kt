package de.akquinet.timref.registrationservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "messengerproxy")
data class MessengerProxyConfig(
    val scheme: String,
    val hostNamePrefix: String,
    val hostNameSuffix: String,
    val actuatorPort: String,
    val actuatorLoggingBasePath: String
)