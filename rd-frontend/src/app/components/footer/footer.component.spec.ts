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
import {HttpClientModule} from '@angular/common/http';
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

  it('should contain link to site-notice', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const divSiteNotice = compiled.querySelector('#site-notice');
    const linkToSiteNotice = divSiteNotice!.getElementsByTagName("a")
    expect(linkToSiteNotice[0].href).toBe("https://akquinet.com/impressum.html");
    expect(linkToSiteNotice[0].textContent).toBe("Impressum & Datenschutz");
  });
});
