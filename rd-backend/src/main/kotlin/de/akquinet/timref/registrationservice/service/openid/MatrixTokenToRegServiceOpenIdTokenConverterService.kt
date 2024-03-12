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

import de.akquinet.timref.registrationservice.config.TokenConfig
import de.akquinet.timref.registrationservice.persistance.orgAdmin.model.extractSynapseServerNameFromMxId
import de.akquinet.timref.registrationservice.service.orgadmin.OrgAdminManagementService
import org.slf4j.Logger
import org.springframework.stereotype.Service

enum class TokenConverterResultErrorType {
    USER_INPUT,
    UNAUTHORIZED
}

sealed class TokenConvertResult

data class SuccessResult(
    val accessToken: String,
    val expiresIn: Long
) : TokenConvertResult()

data class ErrorResult(
    val errorType: TokenConverterResultErrorType,
    val message: String? = null
) : TokenConvertResult()

@Service
class MatrixTokenToRegServiceOpenIdTokenConverterService(
    private val logger: Logger,
    private val tokenCreator: RegServiceOpenIdTokenCreatorService,
    private val matrixTokenValidator: MatrixTokenValidatorService,
    private val tokenConfig: TokenConfig,
    private val orgAdminService: OrgAdminManagementService
) {

    fun convertTokenForUser(
        userId: String,
        requestToken: String
    ): TokenConvertResult {
        val synapseServerName = extractSynapseServerNameFromMxId(userId)

        if (synapseServerName.isNullOrEmpty()) {
            return ErrorResult(
                TokenConverterResultErrorType.USER_INPUT,
                "bad userId"
            )
        }

        val synapseAdminUserId = try {
            matrixTokenValidator.validateToken(userId, requestToken, synapseServerName)
        } catch (e: InvalidTokenException) {
            logger.warn("Validation of Matrix token failed", e)
            return ErrorResult(TokenConverterResultErrorType.UNAUTHORIZED)
        }

        val validatedOrgAdminEntity = orgAdminService.getByMxId(synapseAdminUserId)
        return if (validatedOrgAdminEntity != null) {
            SuccessResult(
                tokenCreator.createToken(validatedOrgAdminEntity),
                tokenConfig.validitySeconds
            )
        } else {
            logger.warn("Validation of Matrix token failed")
            ErrorResult(TokenConverterResultErrorType.UNAUTHORIZED)
        }
    }

}
