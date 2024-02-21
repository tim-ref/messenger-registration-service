package de.akquinet.timref.registrationservice.unitTests.service.messengerservice

import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceServiceImpl
import de.akquinet.timref.registrationservice.config.MessengerProxyConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.net.URI

class MessengerInstanceServiceTest : DescribeSpec() {

    init {
        describe("MessengerInstanceServiceTest") {

            it("can build proxy url") {
                val messengerProxyConfig = MessengerProxyConfig(
                    scheme = "http://",
                    actuatorPort = "1233",
                    hostNamePrefix = "synapse-messengerproxy",
                    hostNameSuffix = "svc.cluster.local",
                    actuatorLoggingBasePath = "/actuator/logging"
                )

                val sut = MessengerInstanceServiceImpl(
                    messengerInstanceCreateService = mockk {},
                    messengerInstanceRepository = mockk {},
                    rawdataService = mockk {},
                    federationService = mockk {},
                    userService = mockk {},
                    regServiceConfig = mockk {},
                    keycloak = mockk {},
                    keycloakAdminConfig = mockk {},
                    realmTemplate = mockk {},
                    operatorConfig = mockk {
                        every { username } returns "operatorUser"
                        every { password } returns "operatorPassword"
                    },
                    orgAdminManagementService = mockk {},
                    messengerProxyConfig = messengerProxyConfig
                )

                val actual = sut.buildInternalProxyInstanceUrl("localhost", "DEBUG/ROOT")
                val expected = URI.create("http://synapse-messengerproxy.localhost.svc.cluster.local:1233/actuator/logging/DEBUG/ROOT")

                actual shouldBe expected
            }
        }
    }
}
