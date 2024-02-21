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
import {ToolbarComponent} from './toolbar.component';
import {HttpClientModule} from '@angular/common/http';
import {AppRoutingModule} from '../../app-routing.module';
import {BrowserModule} from '@angular/platform-browser';
import {ButtonModule} from '../button/button.module';
import {DialogModule} from '../dialog/dialog.module';
import {MessengerInstanceModule} from '../../modules/events/messenger-instance.module';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {TranslateModule} from '@ngx-translate/core';
import {KeycloakAngularModule} from "keycloak-angular";

describe('ToolbarComponent', () => {
  let fixture: ComponentFixture<ToolbarComponent>;
  let component: ToolbarComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ToolbarComponent],
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
      providers: [],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render toolbar keycloak buttons', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#account-page-button')).toBeTruthy();
    expect(compiled.querySelector('#logout-button')).toBeTruthy();
  });

  it('should render toolbar images', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#akquinet-logo')).toBeTruthy();
    expect(compiled.querySelector('#gematik-logo')).toBeTruthy();
  });
});
