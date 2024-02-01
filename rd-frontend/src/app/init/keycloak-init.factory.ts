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

import { KeycloakEventType, KeycloakService } from 'keycloak-angular';
import { environment } from '../../environments/environment';
import { filter } from 'rxjs';

export function initializeKeycloak(
  keycloak: KeycloakService
): () => Promise<any> {
  return (): Promise<any> => {
    return new Promise( (resolve, reject) => {
      (async () => {
        try {
          await keycloak.init({
            config: {
              url: environment.keycloakUrl,
              realm: environment.keycloakRealm,
              clientId: environment.keycloakClientId,
            },
            initOptions: {
              redirectUri: environment.redirectUri,
              checkLoginIframe: false,
            },
            bearerExcludedUrls: [environment.fachdienstMetaUrl],
          });

          // If the refresh token expired, re-login to get new tokens if session is still active or ask to re-authenticate
          keycloak.keycloakEvents$
              .pipe(
                  filter(
                      (event) => event.type === KeycloakEventType.OnAuthRefreshError
                  )
              )
              .subscribe(() => {
                keycloak.login();
              });

          resolve(true);
        } catch (error) {
          reject(error);
        }
      })()
    });
  };
}
