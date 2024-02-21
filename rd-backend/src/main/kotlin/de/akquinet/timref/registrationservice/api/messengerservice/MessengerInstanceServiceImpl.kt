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

package de.akquinet.timref.registrationservice.api.messengerservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import de.akquinet.timref.registrationservice.api.federation.FederationService
import de.akquinet.timref.registrationservice.api.federation.model.Domain
import de.akquinet.timref.registrationservice.api.messengerservice.MessengerInstanceCreateService.Companion.X_HEADER_INSTANCE_RANDOM
import de.akquinet.timref.registrationservice.api.messengerservice.operatormodels.AdminUser
import de.akquinet.timref.registrationservice.config.KeycloakAdminConfig
import de.akquinet.timref.registrationservice.config.MessengerProxyConfig
import de.akquinet.timref.registrationservice.config.OperatorConfig
import de.akquinet.timref.registrationservice.config.RegServiceConfig
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstance
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceEntity
import de.akquinet.timref.registrationservice.persistance.messengerInstance.MessengerInstanceRepository
import de.akquinet.timref.registrationservice.rawdata.RawdataService
import de.akquinet.timref.registrationservice.rawdata.model.Operation
import de.akquinet.timref.registrationservice.service.orgadmin.OrgAdminManagementService
import de.akquinet.timref.registrationservice.util.TrivialResponseErrorHandler
import de.akquinet.timref.registrationservice.util.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.WebApplicationException
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


@EnableScheduling
@Service
class MessengerInstanceServiceImpl(
    private val operatorConfig: OperatorConfig,
    private val messengerInstanceRepository: MessengerInstanceRepository,
    private val orgAdminManagementService: OrgAdminManagementService,
    private val userService: UserService,
    private val rawdataService: RawdataService,
    private val federationService: FederationService,
    private val keycloak: Keycloak,
    private val keycloakAdminConfig: KeycloakAdminConfig,
    private val regServiceConfig: RegServiceConfig,
    private val messengerProxyConfig: MessengerProxyConfig,
    private val messengerInstanceCreateService: MessengerInstanceCreateService,
    @Value("classpath:realm-template.json") val realmTemplate: Resource
) : MessengerInstanceService {

    private val restTemplate = RestTemplateBuilder().errorHandler(TrivialResponseErrorHandler()).build()
    private val restTemplateWithAuth =
        RestTemplateBuilder().errorHandler(TrivialResponseErrorHandler())
            .basicAuthentication(operatorConfig.username, operatorConfig.password).build()

    private val logger: Logger = LoggerFactory.getLogger(MessengerInstanceServiceImpl::class.java)

    private val gson = Gson()

    private fun basicHeaders() = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        setBasicAuth(operatorConfig.username, operatorConfig.password)
    }

    private fun endDate(dateOfOrder: LocalDate, runtime: Long): LocalDate {
        return dateOfOrder.plusMonths(runtime)
    }

    private fun noNewInstances(endDate: LocalDate, instances: Int): ResponseEntity<String> {

        return if (LocalDate.now().isAfter(endDate)) {
            ResponseEntity(
                "Die maximale Laufzeit Ihrer Messenger-Services ist bereits erreicht: Sie können keine weiteren Instanzen bestellen",
                HttpStatus.PRECONDITION_FAILED
            )
        } else if (instances == userService.loadUserAttributeByClaim("instances")!!.toInt()) {
            ResponseEntity(
                "Die maximale Anzahl Messenger-Service Instanzen ist bereits erreicht: Sie können keine weiteren Instanzen bestellen",
                HttpStatus.FORBIDDEN
            )
        } else {
            ResponseEntity(HttpStatus.OK)
        }
    }

    private fun getResponseString(statusCode: HttpStatus, serverName: String): String {
        return when (statusCode) {
            HttpStatus.CONFLICT -> "\"An instance with the selected server name or base URL already exists\""

            HttpStatus.INTERNAL_SERVER_ERROR -> "\"Error on creating new instance\""

            HttpStatus.NOT_FOUND -> "\"The user was not found\""

            else -> "\"Erstelle Instanz mit Servername: ${serverName} und URL: ${serverName}\""
        }
    }

    @Scheduled(cron = "0 0 22 * * *")
    fun deleteEndOfLifeReachedInstances() {
        val instancesToDelete = messengerInstanceRepository.findAll().filter {
            it.endDate.isBefore(LocalDate.now())
        }

        instancesToDelete.forEach {
            deleteInstance(it.serverName, it.userId)
        }
    }

    @ExperimentalTime
    override fun createNewInstance(
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<String> {
        val userId = userService.getUserIdFromContext()
        var serverName = ""
        val (responseEntity, elapsed) = measureTimedValue {
            val date = userService.loadUserAttributeByClaim("date_of_order")
                ?: throw IllegalStateException("Cannot create new instance: Claim 'date_of_order' not found")
            val dateOfOrder = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))
            logger.debug("User Date of Order {}", dateOfOrder)
            val endDate = endDate(
                dateOfOrder,
                userService.loadUserAttributeByClaim("runtime")?.toLong()
                    ?: throw IllegalStateException("Cannot create new instance: Claim 'runtime' not found")
            )
            logger.debug("User endDate for instances {}", endDate)
            val instancesSize = getAllInstancesForUser().size
            logger.debug("Number of instances for User {}", instancesSize)

            val response = noNewInstances(endDate, instancesSize)
            logger.debug("new Instances? {}", response.statusCode.value())
            if (response.statusCode.value() == HttpStatus.OK.value()) {
                var statusCode = HttpStatus.CREATED

                val telematikId = userService.loadUserAttributeByClaim("telematik_id")
                    ?: throw IllegalStateException("Cannot create new instance: Claim 'telematik_id' not found")
                val nextInstanceRandom =
                    messengerInstanceCreateService.generateAvailableInstanceName(operatorConfig.baseFQDN)
                val nextInstanceName = telematikId.lowercase() + nextInstanceRandom
                val nextInstanceFQDN = nextInstanceName + operatorConfig.baseFQDN

                if (!userId.isNullOrBlank()) {

                    logger.debug("FQDN: {}", nextInstanceFQDN)
                    val messengerInstanceEntityToSave = MessengerInstanceEntity(
                        userId = userId,
                        telematikId = telematikId,
                        professionId = userService.loadUserAttributeByClaim("profession_oid"),
                        instanceId = nextInstanceFQDN.replace(".", ""),
                        dateOfOrder = dateOfOrder,
                        endDate = endDate,
                        serverName = nextInstanceFQDN,
                        publicBaseUrl = nextInstanceFQDN,
                        active = true,
                        startOfInactivity = null
                    )

//                  Create realm inside Keycloak
                    val clientSecret = createRealmKeycloak(messengerInstanceEntityToSave)
                    if (clientSecret.isNotBlank()) {
                        try {
                            messengerInstanceRepository.save(messengerInstanceEntityToSave)
                        } catch (exception: Exception) {
                            statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                            logger.error("Error creating messenger instance: database")
                            deleteRealmKeycloak(messengerInstanceEntityToSave.instanceId)
                        }

                        if (statusCode == HttpStatus.CREATED) {
                            // skip update of vzd federation list in local environment
                            if (regServiceConfig.callExternalServices) {
                                statusCode = try {
                                    federationService.updateVZDFederationList(
                                        Domain(
                                            messengerInstanceEntityToSave.serverName,
                                            false,
                                            messengerInstanceEntityToSave.telematikId
                                        )
                                    ).httpStatus
                                } catch (e: Exception) {
                                    logger.error("Exception creating messenger instance", e)
                                    deleteRealmKeycloak(messengerInstanceEntityToSave.instanceId)
                                    messengerInstanceRepository.delete(messengerInstanceEntityToSave)
                                    HttpStatus.INTERNAL_SERVER_ERROR
                                }
                            } else {
                                statusCode = HttpStatus.OK
                            }

                            if (statusCode == HttpStatus.OK) {
                                // skip creation of new instance in local environment
                                if (regServiceConfig.callExternalServices) {
                                    statusCode = createInstanceOperator(
                                        messengerInstanceEntityToSave,
                                        clientSecret,
                                        keycloakAdminConfig.url + "realms/${messengerInstanceEntityToSave.instanceId}"
                                    )

                                    if (statusCode != HttpStatus.CREATED) {
                                        logger.error("Error creating messenger instance: operator")
                                        statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                                        deleteRealmKeycloak(messengerInstanceEntityToSave.instanceId)
                                        messengerInstanceRepository.delete(messengerInstanceEntityToSave)
                                        federationService.deleteDomainFromVZDFederationList(
                                            messengerInstanceEntityToSave.serverName
                                        )
                                    }
                                } else {
                                    statusCode = HttpStatus.CREATED
                                }
                            } else {
                                logger.error("Error creating messenger instance: vzd")
                                deleteRealmKeycloak(messengerInstanceEntityToSave.instanceId)
                                messengerInstanceRepository.delete(messengerInstanceEntityToSave)
                                statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                            }
                        }
                    } else {
                        logger.error("Error creating messenger instance: keycloak realm")
                        deleteRealmKeycloak(messengerInstanceEntityToSave.instanceId)
                        statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }

                val responseString = getResponseString(statusCode, nextInstanceName)

                serverName = nextInstanceName
                ResponseEntity
                    .status(statusCode)
                    .header(X_HEADER_INSTANCE_RANDOM, nextInstanceRandom)
                    .body(responseString)
            } else {
                response
            }
        }

        rawdataService.collectAndSendRawData(
            httpServletRequest.getHeader("Content-Length")?.toIntOrNull() ?: 0,
            responseEntity.body?.length ?: 0,
            responseEntity.statusCode,
            elapsed,
            Operation.RS_CREATE_MESSENGER_SERVICE,
            serverName
        )
        return responseEntity
    }

    override fun deleteInstance(serverName: String, userId: String?): ResponseEntity<String> {
        val instanceEntity: MessengerInstanceEntity? = if (userId.isNullOrEmpty()) {
            messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
                serverName, userService.getUserIdFromContext()!!
            )
        } else {
            messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
                serverName, userId
            )
        }
        var statusCode = HttpStatus.NOT_FOUND
        var responseString = "\"Messenger Instance not found\""
        if (instanceEntity != null) {
            val vzdSuccess = if (regServiceConfig.callExternalServices) {
                try {
                    federationService.deleteDomainFromVZDFederationList(serverName).httpStatus == HttpStatus.NO_CONTENT
                } catch (e: Exception) {
                    logger.error("Exception deleting messenger instance: vzd")
                    false
                }
            } else {
                true
            }
            val orgAdminSuccess = try {
                val orgAdmin = orgAdminManagementService.getByServerName(instanceEntity.serverName)
                if (orgAdmin != null) {
                    orgAdminManagementService.delete(orgAdmin)
                }

                true
            } catch (e: Exception) {
                logger.error("Exception deleting orgadmin for ${instanceEntity.serverName}")
                false
            }

//          skip deletion of new instance in local and test environment
            val operatorSuccess = if (regServiceConfig.callExternalServices) {
                deleteInstanceOperator(instanceEntity) == HttpStatus.OK
            } else {
                true
            }
            val entitySuccess = try {
                messengerInstanceRepository.delete(instanceEntity)
                true
            } catch (exception: Exception) {
                logger.error("Error deleting messenger instance: entity in db for ${instanceEntity.serverName}")
                false
            }

//          delete keycloak realm
            val keycloakSuccess = try {
                this.deleteRealmKeycloak(instanceEntity.instanceId) == HttpStatus.OK
            } catch (exception: Exception) {
                logger.error("Error deleting keycloak realm for ${instanceEntity.serverName}")
                false
            }
            if (keycloakSuccess && entitySuccess && operatorSuccess && orgAdminSuccess && vzdSuccess) {
                responseString = "\"Delete instance with server name: ${serverName}\""
                statusCode = HttpStatus.OK
            } else {
                responseString = "\"Error on deleting new instance\""
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR
            }
        }

        return ResponseEntity(responseString, statusCode)
    }


    override fun createAdminUser(serverName: String): ResponseEntity<String> {
        val statusCode: HttpStatus
        val instanceEntity = messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
            serverName,
            userService.getUserIdFromContext()!!
        )
        if (instanceEntity != null) {
            val orgAdmin = orgAdminManagementService.getByServerName(instanceEntity.serverName)
            if (orgAdmin != null) {
                statusCode = HttpStatus.CONFLICT
            } else {
                val userBaseData = prepareBaseUserData()

                if (createKeycloakAdminUser(instanceEntity.instanceId, userBaseData)) {
                    val operatorResponse = if (regServiceConfig.callExternalServices) {
                        createAdminOperatorCall(serverName, userBaseData)
                    } else {
                        HttpStatus.CREATED
                    }

                    if (operatorResponse == HttpStatus.CREATED) {
                        orgAdminManagementService.createOrgAdmin(
                            serverName = instanceEntity.serverName,
                            telematikId = instanceEntity.telematikId!!,
                            mxId = "@${userBaseData.userName}:$serverName",
                            professionOid = instanceEntity.professionId!!
                        )
                        return ResponseEntity(gson.toJson(userBaseData), HttpStatus.CREATED)
                    } else {

                        deleteKeycloakUser(serverName, userBaseData)
                        statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                } else {
                    statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                }


            }
        } else {
            statusCode = HttpStatus.NOT_FOUND
        }

        val responseString = when (statusCode) {
            HttpStatus.CONFLICT -> {
                "\"Admin user for for Instance already Exists\""
            }

            HttpStatus.INTERNAL_SERVER_ERROR -> {
                // description of 500 is used in frontend, please change it there as well if you are making changes here
                "\"Error on creating admin user through operator\""
            }

            else -> {
                "\"Messenger Instance could not be found\""

            }
        }
        return ResponseEntity(responseString, statusCode)
    }

    override fun changeLogLevel(serverName: String, logLevel: String, loggerIdentifier: String): ResponseEntity<String> {
        val statusCode: HttpStatus
        val instanceEntity = messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
            serverName,
            userService.getUserIdFromContext()!!
        )
        statusCode = if (instanceEntity != null) {
            val proxyResponse = changeProxyInstanceLogLevel(instanceEntity.instanceId, logLevel.uppercase(), loggerIdentifier)
            if (proxyResponse.is2xxSuccessful) {
                return ResponseEntity(HttpStatus.OK)
            } else {
                HttpStatus.INTERNAL_SERVER_ERROR
            }
        } else {
            HttpStatus.NOT_FOUND
        }

        val responseString = when (statusCode) {
            HttpStatus.INTERNAL_SERVER_ERROR -> {
                // description of 500 is used in frontend, please change it there as well if you are making changes here
                "\"Error on changing log level through operator\""
            }

            HttpStatus.NOT_FOUND -> {
                "\"Messenger Instance could not be found\""
            }

            else -> {
                "\"Interal Server Error in Backend during log level change\""

            }
        }
        return ResponseEntity(responseString, statusCode)
    }

    override fun instanceReadyCheck(serverName: String): ResponseEntity<String> {
        val statusCode: HttpStatus
        val instanceEntity = messengerInstanceRepository.findDistinctFirstByServerNameAndUserId(
            serverName,
            userService.getUserIdFromContext()!!
        )
        statusCode = if (instanceEntity != null) {
            if (regServiceConfig.callExternalServices) {
                operatorInstanceCheck(instanceEntity.serverName)
            } else {
                HttpStatus.OK
            }
        } else {
            HttpStatus.NOT_FOUND
        }

        val responseString = when (statusCode) {

            HttpStatus.OK -> {
                true.toString()
            }

            HttpStatus.INTERNAL_SERVER_ERROR -> {
                // description of 500 is used in frontend, please change it there as well if you are making changes here
                "\"Error during operator instance check\""
            }

            HttpStatus.NOT_FOUND -> {
                "\"Messenger Instance could not be found\""
            }

            else -> {
                "\"Interal Server Error in Backend during instance ready check\""

            }
        }
        return ResponseEntity(responseString, statusCode)
    }

    private fun prepareBaseUserData(): AdminUser {
        val userCharPool: List<Char> = ('a'..'z') + ('0'..'9')
        val passwordCharPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return AdminUser(userName = List(8) { userCharPool.random() }.joinToString(""),
            password = List(16) { passwordCharPool.random() }.joinToString("")
        )
    }

    private fun operatorInstanceCheck(serverName: String): HttpStatus {
        if (regServiceConfig.callExternalServices) {
            val operatorResponse: ResponseEntity<String> = restTemplateWithAuth.getForEntity(
                operatorConfig.let { "${it.host}:${it.port}${it.createPath}/${serverName.replace(".", "")}/ready" },
                String::class.java
            )
            if (operatorResponse.statusCode != HttpStatus.OK) {
                logger.error("Error during instance check at operator, status: ${operatorResponse.statusCode} , message:  ${operatorResponse.body}")
                return HttpStatus.INTERNAL_SERVER_ERROR
            }
        }
        return HttpStatus.OK

    }

    private fun changeProxyInstanceLogLevel(
        serverName: String,
        logLevel: String,
        loggerIdentifier: String
    ): HttpStatusCode {
        val requestSpecificPath = "$logLevel/$loggerIdentifier"
        val internalProxyInstanceUrl = buildInternalProxyInstanceUrl(serverName, requestSpecificPath)
        val proxyResponse = restTemplate.exchange(
            internalProxyInstanceUrl,
            HttpMethod.PUT,
            HttpEntity.EMPTY,
            String::class.java
        )

        if (proxyResponse.statusCode.is2xxSuccessful) {
            logger.info("Changed log level to {} for logger {} at proxy instance {}", logLevel, loggerIdentifier, serverName)
            return HttpStatus.OK
        }

        logger.error("Error changing instance logLevel through operator, status: ${proxyResponse.statusCode}")
        return HttpStatus.INTERNAL_SERVER_ERROR
    }

    private fun createAdminOperatorCall(serverName: String, user: AdminUser): HttpStatus {
        if (regServiceConfig.callExternalServices) {

            val userJson = gson.toJson(user)
            val request: HttpEntity<String> = HttpEntity<String>(userJson, basicHeaders())
            val operatorResponse: ResponseEntity<String> = restTemplate.postForEntity(
                operatorConfig.let { "${it.host}:${it.port}${it.createPath}/${serverName.replace(".", "")}/admin" },
                request,
                String::class.java
            )
            if (operatorResponse.statusCode != HttpStatus.CREATED) {
                logger.error("Error creating instance through operator, status: " + operatorResponse.statusCode + ", message: " + operatorResponse.body)
                return HttpStatus.INTERNAL_SERVER_ERROR
            }
        }

        return HttpStatus.CREATED
    }

    fun createKeycloakAdminUser(serverName: String, user: AdminUser): Boolean {
        val password = preparePasswordRepresentation(user.password)
        val userName = prepareUserRepresentation(user.userName, password)

        return try {
            val realmResource = keycloak.realm(serverName)
            val usersRessource = realmResource.users()
            val response = usersRessource.create(userName)
            if (response.status == HttpStatus.CREATED.value()) {
                return try {
                    // add client roles to newly created user
                    // the roles cannot be added before or on user creation
                    val userId = CreatedResponseUtil.getCreatedId(response)
                    val createdUserResource = usersRessource[userId]

                    val realmManagementClient = realmResource.clients()
                        .findByClientId("realm-management")[0]

                    val manageUsersRole = realmResource.clients()[realmManagementClient.id]
                        .roles()["manage-users"].toRepresentation()

                    val viewUsersRole = realmResource.clients()[realmManagementClient.id]
                        .roles()["view-users"].toRepresentation()

                    createdUserResource.roles()
                        .clientLevel(realmManagementClient.id).add(listOf(manageUsersRole, viewUsersRole))
                    true
                } catch (e: Exception) {
                    logger.error("Error adding roles to keycloak admin", e)
                    deleteKeycloakUser(serverName, user)

                    false
                }
            } else {
                logger.error(
                    "Error creating keycloak user, status: " + response.status + ", message: " + response.readEntity(
                        String::class.java
                    )
                )
                false
            }
        } catch (e: WebApplicationException) {
            logger.error(
                "Error creating keycloak user, status: " + e.response.status + ", message: " + e.response.readEntity(
                    String::class.java
                )
            )
            return false
        }
    }

    private fun deleteKeycloakUser(serverName: String, adminUser: AdminUser) {
        val userId = keycloak.realm(serverName).users().search(adminUser.userName)[0].id
        keycloak.realm(serverName).users().delete(userId)
    }

    private fun preparePasswordRepresentation(
        password: String
    ): CredentialRepresentation {
        val credentialRepresentation = CredentialRepresentation()
        credentialRepresentation.isTemporary = false
        credentialRepresentation.type = CredentialRepresentation.PASSWORD
        credentialRepresentation.value = password
        return credentialRepresentation
    }

    private fun prepareUserRepresentation(
        userName: String, cR: CredentialRepresentation
    ): UserRepresentation {
        val newUser = UserRepresentation()
        newUser.username = userName
        newUser.credentials = listOf(cR)
        newUser.isEnabled = true
        newUser.email = userService.loadUserAttributeByClaim("email")
        return newUser
    }

    override fun getAllInstancesForUser(): List<MessengerInstance> {
        return messengerInstanceRepository.findAllByUserId(userService.getUserIdFromContext()!!).map {
            it.toMessengerInstance()
        }
    }

    private fun createRealmKeycloak(
        messengerInstanceEntity: MessengerInstanceEntity
    ): String {
        val mapper = ObjectMapper()
        val realm: RealmRepresentation =
            mapper.readValue(this.realmTemplate.inputStream, RealmRepresentation::class.java)
        // set realm name
        realm.realm = messengerInstanceEntity.instanceId
        // set synapse url
        realm.clients.find { clientRepresentation -> clientRepresentation.clientId == "synapse" }?.rootUrl =
            "https://${messengerInstanceEntity.publicBaseUrl}"
        // configure smtp server
        realm.smtpServer["from"] = this.keycloakAdminConfig.smtp.from
        realm.smtpServer["fromDisplayName"] = this.keycloakAdminConfig.smtp.fromDisplayName
        realm.smtpServer["password"] = this.keycloakAdminConfig.smtp.password

        return try {
            keycloak.realms().create(realm)
            // refresh token to get new roles for the new realm
            keycloak.tokenManager().refreshToken()
            // get generated client-secret from keycloak
            keycloak.realm(messengerInstanceEntity.instanceId).clients().findByClientId("synapse").last().secret
        } catch (e: WebApplicationException) {
            logger.error(
                "Error creating new keycloak realm, status: " + e.response.status + ", message: " + e.response.readEntity(
                    String::class.java
                )
            )
            ""
        } catch (e: Exception) {
            logger.error("Error creating new keycloak realm", e)
            ""
        }
    }

    private fun deleteRealmKeycloak(serverName: String): HttpStatus {
        return try {
            keycloak.realm(serverName).remove()
            HttpStatus.OK
        } catch (e: WebApplicationException) {
            logger.error(
                "Error deleting keycloak realm, status: " + e.response.status + ", message: " + e.response.readEntity(
                    String::class.java
                )
            )
            HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    private fun createInstanceOperator(
        messengerInstanceEntity: MessengerInstanceEntity, clientSecret: String, issuer: String
    ): HttpStatus {
        val config = gson.toJson(messengerInstanceEntity.toSynapseSpec(clientSecret, issuer))
        logger.debug("Config sent to Operator: $config")
        val request: HttpEntity<String> = HttpEntity<String>(config, basicHeaders())
        val operatorResponse: ResponseEntity<String> = restTemplate.postForEntity(
            operatorConfig.let { "${it.host}:${it.port}${it.createPath}" }, request, String::class.java
        )

        if (operatorResponse.statusCode != HttpStatus.CREATED) {
            logger.error("Error creating instance through operator, status: " + operatorResponse.statusCode + ", message: " + operatorResponse.body)
            return HttpStatus.INTERNAL_SERVER_ERROR
        }
        return HttpStatus.CREATED
    }

    private fun deleteInstanceOperator(
        messengerInstanceEntity: MessengerInstanceEntity
    ): HttpStatus {
        val request: HttpEntity<*> = HttpEntity<Any>(basicHeaders())
        logger.info("Headers {}", request.headers.toString())
        logger.info("Body {}", request.body)
        val operatorResponse: ResponseEntity<String> = restTemplate.exchange(
            operatorConfig.let { "${it.host}:${it.port}${it.deletePath}/${messengerInstanceEntity.instanceId}" },
            HttpMethod.DELETE,
            request,
            String::class.java
        )

        if (operatorResponse.statusCode != HttpStatus.OK) {
            logger.error("Error deleting instance through operator, status: " + operatorResponse.statusCode + ", message: " + operatorResponse.body)
            return HttpStatus.INTERNAL_SERVER_ERROR
        }
        return HttpStatus.OK
    }

    fun buildInternalProxyInstanceUrl(serverName: String, specificPath: String): URI {
        val scheme = messengerProxyConfig.scheme
        val host = "${messengerProxyConfig.hostNamePrefix}.$serverName.${messengerProxyConfig.hostNameSuffix}"
        val port = messengerProxyConfig.actuatorPort
        val path = "${messengerProxyConfig.actuatorLoggingBasePath}/$specificPath"
        return URI.create("$scheme$host:$port$path")
    }
}
