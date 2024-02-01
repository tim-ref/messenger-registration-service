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

export const environment = {
  production: true,
  apiUrl: 'NOT_SET_API_URL',
  keycloakRealm: 'timref',
  keycloakClientId: 'registrationservice-frontend',
  keycloakUrl: 'NOT_SET_KEYCLOAK_URL',
  redirectUri: 'NOT_SET_REDIRECT_URI',
  version: require('../../tmp/version'),
  accountUrl: 'NOT_SET_KEYCLOAK_URL/realms/timref/account/#/personal-info',
  orgAdminUri: 'NOT_SET_ORG_ADMIN_URI',
  fachdienstMetaUrl: 'NOT_SET_FACHDIENST_META_URL'
};
