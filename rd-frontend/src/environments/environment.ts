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

// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.kubernetes.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  fachdienstMetaUrl: 'http://host.docker.internal:8070',
  apiUrl: 'http://localhost:8080',
  keycloakRealm: 'timref',
  keycloakClientId: 'registrationservice-frontend',
  keycloakUrl: 'http://host.docker.internal:8180',
  redirectUri: 'http://localhost:4200/',
  version: require('../../tmp/version'),
  accountUrl:
    'http://host.docker.internal:8180/realms/timref/account/#/personal-info',
  orgAdminUri: 'http://localhost:8280/',
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
