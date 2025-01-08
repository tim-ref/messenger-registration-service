/*
 * Copyright (C) 2024-2024 akquinet GmbH
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

package de.akquinet.tim.registrationservice.api.keycloak

import org.springframework.http.HttpStatus

enum class KeycloakOperationResult ( val httpStatus: HttpStatus) {
    REALM_CREATED(HttpStatus.CREATED),
    REALM_NOT_CREATED(HttpStatus.INTERNAL_SERVER_ERROR),
    REALM_ALREADY_PRESENT(HttpStatus.OK),
    REALM_DELETED(HttpStatus.NO_CONTENT),
    REALM_NOT_DELETED(HttpStatus.INTERNAL_SERVER_ERROR),
    REALM_NOT_FOUND(HttpStatus.NOT_FOUND);
}