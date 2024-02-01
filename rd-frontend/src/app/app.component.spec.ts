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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { ToolbarComponent } from './components/toolbar/toolbar.component';
import { AppRoutingModule } from './app-routing.module';
import { BrowserModule } from '@angular/platform-browser';
import { ButtonModule } from './components/button/button.module';
import { DialogModule } from './components/dialog/dialog.module';
import { MessengerInstanceModule } from './modules/events/messenger-instance.module';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TimeoutInterceptor } from './interceptors/timeout.interceptor';
import { APP_INITIALIZER } from '@angular/core';
import { initializeKeycloak } from './init/keycloak-init.factory';
import { AppAuthguard } from './authGuard/app.authguard';
import { ToastComponent } from './components/toast/toast.component';
import {FooterComponent} from "./components/footer/footer.component";

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;

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
          useFactory: initializeKeycloak,
          multi: true,
          deps: [KeycloakService],
        },
        AppAuthguard,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });
});
