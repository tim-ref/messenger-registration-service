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

import { Component, OnInit } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { AppService } from '../../services/app.service';
import { I18nService } from '../../services/i18n.service';
import de from './assets/de.json';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'admin-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss'],
})
export class ToolbarComponent implements OnInit {
  constructor(
    private keycloakService: KeycloakService,
    private appService: AppService,
    private i18nService: I18nService
  ) {}

  ngOnInit() {
    this.i18nService.addTranslation('de', de);
  }

  openAccountPage() {
    window.open(environment.accountUrl, '_blank')?.focus();
  }

  openOrgAdminApp() {
    window.open(environment.orgAdminUri, '_blank')?.focus();
  }

  logout() {
    this.keycloakService.logout(environment.redirectUri).catch((error) => {
      this.appService.showToast({
        icon: 'fa-triangle-exclamation',
        iconColor: 'primary',
        description: 'ADMIN.ERROR.LOGOUT',
        timeout: 5000,
      });

      console.error(error.code + ' ' + error.reason + '. ' + error.message);
    });
  }
}
