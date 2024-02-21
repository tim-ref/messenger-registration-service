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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AppComponent} from './app.component';
import {ToolbarComponent} from './components/toolbar/toolbar.component';
import {AppRoutingModule} from './app-routing.module';
import {BrowserModule} from '@angular/platform-browser';
import {ButtonModule} from './components/button/button.module';
import {DialogModule} from './components/dialog/dialog.module';
import {MessengerInstanceModule} from './modules/events/messenger-instance.module';
import {HTTP_INTERCEPTORS, HttpClient, HttpStatusCode} from '@angular/common/http';
import {TranslateModule} from '@ngx-translate/core';
import {KeycloakAngularModule, KeycloakService} from 'keycloak-angular';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {TimeoutInterceptor} from './interceptors/timeout.interceptor';
import {APP_INITIALIZER, InjectionToken} from '@angular/core';
import {initializeKeycloak} from './init/keycloak.factory';
import {AppAuthguard} from './authGuard/app.authguard';
import {ToastComponent} from './components/toast/toast.component';
import {FooterComponent} from "./components/footer/footer.component";
import {appConfigurationFactory} from "./init/app-config.factory";
import {AppConfigurationService} from "./services/appConfiguration.service";
import {AppConfig} from "./models/appConfig";
import {HttpClientTestingModule, HttpTestingController} from "@angular/common/http/testing";

const ConfigDependentServices = new InjectionToken<(() => Function)[]>('ConfigDependentServices');

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AppComponent, ToolbarComponent, ToastComponent,FooterComponent],
      imports: [
        AppRoutingModule,
        KeycloakAngularModule,
        BrowserModule,
        ButtonModule,
        DialogModule,
        MessengerInstanceModule,
        HttpClientTestingModule,
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
        AppAuthguard,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    httpMock = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;

    // mock app config GET request
    const getAppConfigRequest = httpMock.expectOne("/runtimeconfig.json");
    expect(getAppConfigRequest.request.method).toBe('GET');
    getAppConfigRequest.flush(new AppConfig(), {
      status: HttpStatusCode.Ok,
      statusText: "passt schon"
    });
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });
});
