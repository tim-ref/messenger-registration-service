package de.akquinet.timref.registrationservice.unitTests.service.messengerservice

import de.akquinet.timref.registrationservice.api.messengerproxy.MessengerProxyLogLevelService
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceService
import de.akquinet.timref.registrationservice.config.MessengerProxyConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.net.URI

class MessengerProxyLogLevelServiceTest : DescribeSpec() {

    init {
        describe("MessengerInstanceServiceTest") {

            it("can build proxy url") {
                val sut = MessengerProxyLogLevelService(
                    logger = mockk {},
                    messengerProxyConfig = MessengerProxyConfig(
                        scheme = "http://",
                        actuatorPort = "1233",
                        hostNamePrefix = "synapse-messengerproxy",
                        hostNameSuffix = "svc.cluster.local",
                        actuatorLoggingBasePath = "/actuator/logging"
                    ),
                    userService = mockk {},
                    messengerInstanceRepository = mockk {},
                    restTemplate = mockk {}
                )

                val actual = sut.buildInternalProxyInstanceUrl("localhost", "DEBUG/ROOT")
                val expected = URI.create("http://synapse-messengerproxy.localhost.svc.cluster.local:1233/actuator/logging/DEBUG/ROOT")

                actual shouldBe expected
            }
        }
    }
}
