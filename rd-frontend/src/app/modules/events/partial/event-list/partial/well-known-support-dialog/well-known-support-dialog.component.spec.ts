/*
 * Copyright (C) 2025 akquinet GmbH
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

import {CommonModule} from "@angular/common";
import {HttpStatusCode} from "@angular/common/http";
import {HttpClientTestingModule, HttpTestingController} from "@angular/common/http/testing";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {By} from "@angular/platform-browser";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {WellKnownSupportService} from 'build/openapi/wellknownsupport';
import {AppServiceStub, DialogServiceStub, TranslateServiceStub} from "../../../../../../../test/stubs";
import {ButtonModule} from "../../../../../../components/button/button.module";
import {DialogModule} from "../../../../../../components/dialog/dialog.module";
import {InputModule} from "../../../../../../components/input/input.module";
import {LoadingIndicatorModule} from "../../../../../../components/loading-indicator/loading-indicator.module";
import {SelectModule} from "../../../../../../components/select/select.module";
import ApiRoutes from "../../../../../../resources/api/api-routes";
import {AppService} from "../../../../../../services/app.service";
import {AppConfigurationService} from "../../../../../../services/appConfiguration.service";
import {DialogService} from "../../../../../../services/dialog.service";
import {WellKnownSupportDialogComponent} from './well-known-support-dialog.component';

describe('WellKnownSupportDialogComponent', () => {
  let component: WellKnownSupportDialogComponent;
  let fixture: ComponentFixture<WellKnownSupportDialogComponent>;
  let appService: AppService;
  let appConfigService: AppConfigurationService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [WellKnownSupportDialogComponent],
      imports: [
        ButtonModule,
        LoadingIndicatorModule,
        CommonModule,
        InputModule,
        SelectModule,
        DialogModule,
        FormsModule,
        HttpClientTestingModule,
        ReactiveFormsModule,
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: DialogService, useClass: DialogServiceStub },
        { provide: AppService, useClass: AppServiceStub },
        { provide: TranslateService, useClass: TranslateServiceStub }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WellKnownSupportDialogComponent);
    component = fixture.componentInstance;

    TestBed.inject(DialogService);
    appService = TestBed.inject(AppService);

    appConfigService = TestBed.inject(AppConfigurationService);
    TestBed.inject(WellKnownSupportService);
    httpMock = TestBed.inject(HttpTestingController);

    component.data = ["serverName"]

    fixture.detectChanges();
  });

  it('should render support page input field', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#supportpage-input')).toBeDefined();
  });

  it('should render add contact button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#add-contact-button')).toBeDefined();
  });

  it('should render save button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#save-support-info-button')).toBeDefined();
  });

  it('click add contact button, should add contact form', ()=> {
    expect(component.contacts).toHaveSize(0);

    fixture.debugElement
      .query(By.css('#add-contact-button'))
      .triggerEventHandler('click', null);

    expect(component.contacts).toHaveSize(1);
  });

  it('click remove icon on contact, should remove icon', ()=> {
    expect(component.contacts).toHaveSize(0);

    component.addContact()
    component.addContact()

    expect(component.contacts).toHaveSize(2);

    fixture.detectChanges()

    fixture.debugElement
      .query(By.css('#contact-1-remove-button'))
      .triggerEventHandler('click', null);

    expect(component.contacts).toHaveSize(1);
  });

  it('should call saveSettings', () => {
    spyOn(component, 'onSubmit').and.callThrough();
    expect(component.onSubmit).not.toHaveBeenCalled();

    fixture.debugElement
      .query(By.css('#save-support-info-button'))
      .triggerEventHandler('click', null);

    expect(component.onSubmit).toHaveBeenCalled();
  });

  it('should show toast with success message on save', () => {
    spyOn(appService, 'showToast');

    // Add page to make form valid
    component.form.get('page')?.setValue('test')

    component.onSubmit();

    const requests = httpMock.match(
      appConfigService.appConfig.apiUrl + ApiRoutes.wellKnownSupport("serverName")
    );
    expect(requests).toHaveSize(2);

    // Simulate a success response
    requests.find(req => req.request.method === 'PUT')
      ?.flush({}, {status: HttpStatusCode.Ok, statusText: 'OK'});

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.SUCCESS',
      icon: 'fa-check',
      iconColor: 'green',
      timeout: 5000,
    });
  });

  it('should show toast with the correct error message on loadSettings error', () => {
    spyOn(appService, 'showToast');

    const req = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.wellKnownSupport("serverName")
    );

    // Simulate an internal server error response
    req.flush({}, {status: HttpStatusCode.InternalServerError, statusText: 'Internal Server Error'});

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.INTERNAL_SERVER_ERROR',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should show toast with the correct error message on save with error', () => {
    spyOn(appService, 'showToast');

    // Add page to make form valid
    component.form.get('page')?.setValue('test')

    component.onSubmit();

    const requests = httpMock.match(
      appConfigService.appConfig.apiUrl + ApiRoutes.wellKnownSupport("serverName")
    );

    // Simulate a BadRequest response as sample for known errors
    requests.find(req => req.request.method === 'PUT')
      ?.flush({}, {status: HttpStatusCode.BadRequest, statusText: 'Bad Request'});

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.BAD_REQUEST',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

});
