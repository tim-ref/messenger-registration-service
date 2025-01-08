/*
 * Copyright (C) 2023-2024 akquinet GmbH
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

package de.akquinet.tim.registrationservice.util

import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus

@Service
class UserService {
    private fun getPrincipal(): Any? {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return when (principal) {
            is Jwt -> principal
            is User -> principal
            else -> null
        }
    }


    fun getUserIdFromContext(): String {
        return when (val principal = getPrincipal()) {
            is Jwt -> principal.getClaimAsString("preferred_username")
            is User -> principal.username
            else -> throw NotLoggedInException()
        }
    }

    fun getUserIdFromContextSafely(): String = try {
        getUserIdFromContext()
    }catch (e: NotLoggedInException) {
        "TIM-registration-service"
    }

    fun loadUserAttributeByClaim(claim: String): String {
        val principal = getPrincipal()
        return when (principal) {
            is Jwt -> principal.getClaimAsString(claim) ?: throw ClaimNotFoundException("Claim $claim was not found.")
            else -> throw ClaimNotFoundException("Claim $claim was not found.")
        }
    }
}

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
class NotLoggedInException(message: String = "No user could be found in security context.") : RuntimeException(message)

@ResponseStatus(value = HttpStatus.FORBIDDEN)
class ClaimNotFoundException(message: String) : RuntimeException(message)