/*
 * Copyright (C) 2024 akquinet GmbH
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

import {ComponentFixture, TestBed} from "@angular/core/testing";
import {HttpClientTestingModule, HttpTestingController} from "@angular/common/http/testing";
import {AppConfigurationService} from "../../../../../../services/appConfiguration.service";
import {DialogService} from "../../../../../../services/dialog.service";
import {ButtonModule} from "../../../../../../components/button/button.module";
import {LoadingIndicatorModule} from "../../../../../../components/loading-indicator/loading-indicator.module";
import {CommonModule} from "@angular/common";
import {InputModule} from "../../../../../../components/input/input.module";
import {DialogModule} from "../../../../../../components/dialog/dialog.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {
  AppServiceStub, DialogServiceStub, TranslateServiceStub
} from "../../../../../../../test/stubs";
import {AppService} from "../../../../../../services/app.service";
import {By} from "@angular/platform-browser";
import ApiRoutes from "../../../../../../resources/api/api-routes";
import {AuthorizationConceptDialogComponent} from "./authorization-concept-dialog.component";
import {HttpStatusCode} from "@angular/common/http";
import {MessengerInstanceService} from "../../../../../../../../build/openapi/messengerinstance";

describe('TimAuthorizationConfigDialogComponent', () => {
  let fixture: ComponentFixture<AuthorizationConceptDialogComponent>;
  let component: AuthorizationConceptDialogComponent;
  let dialogService: DialogService;
  let appService: AppService;
  let appConfigService: AppConfigurationService;
  let messengerInstanceService: MessengerInstanceService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AuthorizationConceptDialogComponent],
      imports: [ButtonModule, LoadingIndicatorModule, CommonModule, InputModule, DialogModule, FormsModule, HttpClientTestingModule, LoadingIndicatorModule, ReactiveFormsModule, TranslateModule.forRoot(),],
      providers: [{provide: DialogService, useClass: DialogServiceStub}, {
        provide: AppService, useClass: AppServiceStub
      }, {provide: TranslateService, useClass: TranslateServiceStub}],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthorizationConceptDialogComponent);
    component = fixture.componentInstance;
    dialogService = TestBed.inject(DialogService);
    appService = TestBed.inject(AppService);

    appConfigService = TestBed.inject(AppConfigurationService);
    messengerInstanceService = TestBed.inject(MessengerInstanceService);
    httpMock = TestBed.inject(HttpTestingController);

    component.data = ["serverName"]

    fixture.detectChanges(); // run ngOnInit
  });

  it('should render input fields', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#concept-in-use')).toBeTruthy();
    expect(compiled.querySelector('#default-config')).toBeTruthy();
  });

  it('should render saveSettings button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#save-auth-config-button')).toBeTruthy();
  });
  it('should call saveSettings', () => {
    spyOn(component, 'saveSettings').and.callThrough();
    expect(component.saveSettings).not.toHaveBeenCalled();

    fixture.debugElement
      .query(By.css('#save-auth-config-button'))
      .triggerEventHandler('click', null);

    expect(component.saveSettings).toHaveBeenCalled();
  });


  it('should show toast with the correct error message on loadSettings error', () => {
    spyOn(appService, 'showToast');


    const req = httpMock.expectOne(appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceTimAuthConceptConfig("serverName"));

    // Simulate an internal server error response
    req.flush({}, {status: HttpStatusCode.InternalServerError, statusText: 'Internal Server Error'});

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.INTERNAL_SERVER_ERROR',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should show toast with the correct error message on saveSettings error', () => {
    spyOn(appService, 'showToast');

    component.formGroup.get('useOldAuthConcept')?.setValue(false);
    component.formGroup.get('useAllowAllAsDefault')?.setValue(false);

    component.saveSettings();

    const requests = httpMock.match(appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceTimAuthConceptConfig("serverName"));

    const postRequest = requests.find(req => req.request.method === 'PUT');

    expect(postRequest).toBeTruthy();

    expect(postRequest!.request.body).toEqual({
      federationCheckConcept: 'CLIENT', inviteRejectionPolicy: 'BLOCK_ALL'
    });


    // Simulate a BadRequest error response
    postRequest!.flush({}, {status: HttpStatusCode.BadRequest, statusText: 'Bad Request'});

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.BAD_REQUEST',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });
});
