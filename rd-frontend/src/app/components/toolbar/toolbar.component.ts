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
import {AppConfigurationService} from "../../services/appConfiguration.service";

@Component({
  selector: 'admin-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss'],
})
export class ToolbarComponent implements OnInit {
  constructor(
    private keycloakService: KeycloakService,
    private appService: AppService,
    private i18nService: I18nService,
    private readonly appConfigService: AppConfigurationService
  ) {}

  ngOnInit() {
    this.i18nService.addTranslation('de', de);
  }

  openAccountPage() {
    let url = this.appConfigService.appConfig.keycloakUrl + environment.accountPath;
    window.open(url, '_blank')?.focus();
  }

  openOrgAdminApp() {
    window.open(this.appConfigService.appConfig.orgAdminUrl, '_blank')?.focus();
  }

  logout() {
    this.keycloakService.logout(this.appConfigService.appConfig.redirectUrl).catch((error) => {
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
