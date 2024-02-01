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
import {FooterComponent} from './footer.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {TimeoutInterceptor} from '../../interceptors/timeout.interceptor';
import {APP_INITIALIZER} from '@angular/core';
import {initializeKeycloak} from '../../init/keycloak-init.factory';
import {KeycloakAngularModule, KeycloakService} from 'keycloak-angular';
import {AppAuthguard} from '../../authGuard/app.authguard';
import {AppRoutingModule} from '../../app-routing.module';
import {BrowserModule} from '@angular/platform-browser';
import {ButtonModule} from '../button/button.module';
import {DialogModule} from '../dialog/dialog.module';
import {MessengerInstanceModule} from '../../modules/events/messenger-instance.module';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {TranslateModule} from '@ngx-translate/core';

describe('FooterComponent', () => {
  let fixture: ComponentFixture<FooterComponent>;
  let component: FooterComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [FooterComponent],
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

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    component.versionList = [
      { component: 'FD', version: '1.0' },
      { component: 'RD', version: '2.0' }
    ];
    fixture.detectChanges();
  });

  it('should create footer component', () => {
    expect(component).toBeTruthy();
  });

  it('should render div elements for each version', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const divElements = compiled.querySelectorAll('.module-info');
    expect(divElements.length).toBe(2);
    expect(divElements[0].textContent).toContain('Fachdienst: 1.0');
    expect(divElements[1].textContent).toContain('Registration Dienst: 2.0');
  });

});
