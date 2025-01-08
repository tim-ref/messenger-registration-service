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

import {APP_INITIALIZER, InjectionToken, NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {KeycloakAngularModule, KeycloakService} from 'keycloak-angular';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {MessengerInstanceModule} from './modules/events/messenger-instance.module';
import {ToolbarComponent} from './components/toolbar/toolbar.component';
import {HTTP_INTERCEPTORS, HttpClient, HttpClientModule} from '@angular/common/http';
import {TimeoutInterceptor} from './interceptors/timeout.interceptor';
import {TranslateModule} from '@ngx-translate/core';
import {ButtonModule} from './components/button/button.module';
import {DialogModule} from './components/dialog/dialog.module';
import {initializeKeycloak} from './init/keycloak.factory';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {AppAuthguard} from './authGuard/app.authguard';
import {ToastComponent} from './components/toast/toast.component';
import {FooterComponent} from "./components/footer/footer.component";
import {appConfigurationFactory} from "./init/app-config.factory";
import {AppConfigurationService} from "./services/appConfiguration.service";
import {BASE_PATH as LOGGING_BASE_PATH, LoggingApiModule} from "../../build/openapi/logging";
import {
  BASE_PATH as MESSENGER_INSTANCE_BASE_PATH,
  MessengerInstanceApiModule
} from "../../build/openapi/messengerinstance";


const ConfigDependentServices = new InjectionToken<(() => Function)[]>('ConfigDependentServices');

@NgModule({
  declarations: [AppComponent, ToolbarComponent, ToastComponent, FooterComponent],
  imports: [
    AppRoutingModule,
    KeycloakAngularModule,
    BrowserModule,
    ButtonModule,
    DialogModule,
    LoggingApiModule,
    MessengerInstanceModule,
    MessengerInstanceApiModule,
    HttpClientModule,
    AppRoutingModule,
    NoopAnimationsModule,
    TranslateModule.forRoot(),
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TimeoutInterceptor,
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: appConfigurationFactory,
      multi: true,
      deps: [HttpClient, AppConfigurationService, ConfigDependentServices],
    },
    {
      provide: ConfigDependentServices,
      useFactory: (
        keycloakService: KeycloakService,
        appConfigService: AppConfigurationService
      ) => {
        return [
          initializeKeycloak(keycloakService, appConfigService)
        ];
      },
      deps: [KeycloakService, AppConfigurationService]
    },
    {
      provide: LOGGING_BASE_PATH,
      useFactory: (appConfigService: AppConfigurationService) => {
        return appConfigService.appConfig.apiUrl + "/backend"
      },
      deps: [AppConfigurationService]
    },
    {
      provide: MESSENGER_INSTANCE_BASE_PATH,
      useFactory: (appConfigService: AppConfigurationService) => {
        return appConfigService.appConfig.apiUrl + "/backend"
      },
      deps: [AppConfigurationService]
    },
    AppAuthguard,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {
}
