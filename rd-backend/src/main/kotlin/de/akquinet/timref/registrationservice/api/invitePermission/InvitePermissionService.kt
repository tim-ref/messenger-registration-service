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

package de.akquinet.timref.registrationservice.api.invitePermission

import com.google.gson.Gson
import de.akquinet.timref.registrationservice.api.federation.FederationServiceImpl
import de.akquinet.timref.registrationservice.config.VZDConfig
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.io.BufferedInputStream


@Service
class InvitePermissionService(
    private val logger: Logger,
    private val federationService: FederationServiceImpl,
    private val vzdConfig: VZDConfig
) {

    private val gson = Gson()

    fun checkUserInvitePermissions(inviter: String, invited: String): Boolean {
        val invitedDirectory = checkUserDirectoryAtVZD(invited)
        logger.info("directory of $invited is $invitedDirectory")
        if (invitedDirectory == "none") return false
        if (invitedDirectory == "org" || invitedDirectory == "orgPract") return true
        val inviterDirectory = checkUserDirectoryAtVZD(inviter)
        logger.info("directory of $inviter is $inviterDirectory")
        return invitedDirectory == "pract" && (inviterDirectory == "pract" || inviterDirectory == "orgPract")
    }


    private fun checkUserDirectoryAtVZD(userString: String): String {
        val paramMap = mutableMapOf<String, Any>()
        paramMap["mxid"] = userString
        val uri = vzdConfig.serviceUrl + vzdConfig.userWhereIsPath
        val connection = federationService.connectToVzd(uri, paramMap)
        val responseBody = BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
        return gson.fromJson(responseBody, String::class.java)
    }
}
