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
import {LogDownloadDialogComponent} from './log-download-dialog.component';
import {AppService} from '../../../../../../services/app.service';
import {HttpClientTestingModule, HttpTestingController,} from '@angular/common/http/testing';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import ApiRoutes from '../../../../../../resources/api/api-routes';
import {environment} from '../../../../../../../environments/environment';
import {ButtonModule} from '../../../../../../components/button/button.module';
import {LoadingIndicatorModule} from '../../../../../../components/loading-indicator/loading-indicator.module';
import {CommonModule} from '@angular/common';
import {InputModule} from '../../../../../../components/input/input.module';
import {DialogModule} from '../../../../../../components/dialog/dialog.module';
import {DialogService} from '../../../../../../services/dialog.service';
import {
  AppConfigurationServiceStub,
  AppServiceStub,
  DialogServiceStub,
  TranslateServiceStub,
} from '../../../../../../../test/stubs';
import {AppConfigurationService} from "../../../../../../services/appConfiguration.service";
import {AppConfig} from "../../../../../../models/appConfig";

describe('LogDownloadDialogComponent', () => {
  let fixture: ComponentFixture<LogDownloadDialogComponent>;
  let component: LogDownloadDialogComponent;
  let dialogService: DialogService;
  let appConfigService: AppConfigurationService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LogDownloadDialogComponent],
      imports: [
        ButtonModule,
        LoadingIndicatorModule,
        CommonModule,
        InputModule,
        DialogModule,
        FormsModule,
        HttpClientTestingModule,
        LoadingIndicatorModule,
        ReactiveFormsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {provide: DialogService, useClass: DialogServiceStub},
        {provide: AppService, useClass: AppServiceStub},
        {provide: AppConfigurationService, useClass: AppConfigurationServiceStub},
        {provide: TranslateService, useClass: TranslateServiceStub}
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogDownloadDialogComponent);
    component = fixture.componentInstance;
    dialogService = TestBed.inject(DialogService);
    appConfigService = TestBed.inject(AppConfigurationService);
    httpMock = TestBed.inject(HttpTestingController);

    component.data = ["component", "serverName"]

    fixture.detectChanges(); // run ngOnInit
  });

  it('should render input fields', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#start-input')).toBeTruthy();
    expect(compiled.querySelector('#timespan-input')).toBeTruthy();
  });

  it('should render logDownload button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(
      compiled.querySelector('#logDownload-button')
    ).toBeTruthy();
  });

  it('should call logDownload', () => {
    spyOn(component, 'logDownload').and.callThrough();
    expect(component.logDownload).not.toHaveBeenCalled();

    fixture.debugElement
      .query(By.css('#logDownload-button'))
      .triggerEventHandler('click', null);

    expect(component.logDownload).toHaveBeenCalled();
  });

  it('should download log with values from form', () => {
    spyOn(dialogService, 'closeDialog');

    component.formGroup.get('start')?.setValue('2023-06-29T05:05:27');
    component.formGroup.get('timespan')?.setValue('30');
    component.logDownload();

    const getRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceLogs + `/${component.data[0]}/${component.data[1]}?start=1688015127&end=1688016927`
    );
    expect(getRequest.request.method).toBe('GET');
  });

});
