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
import {By} from '@angular/platform-browser';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {MessengerInstancesListComponent} from './messenger-instances-list.component';
import {ButtonModule} from '../../../../components/button/button.module';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {InputModule} from '../../../../components/input/input.module';
import {DialogModule} from '../../../../components/dialog/dialog.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {LoadingIndicatorModule} from '../../../../components/loading-indicator/loading-indicator.module';
import {HttpClientTestingModule, HttpTestingController,} from '@angular/common/http/testing';
import {
  DeleteMessengerInstancesDialogComponent
} from './partial/delete-messenger-instance-dialog/delete-messenger-instances-dialog.component';
import {DialogService} from '../../../../services/dialog.service';
import {MessengerInstance} from '../../../../models/messengerInstance';
import ApiRoutes from '../../../../resources/api/api-routes';
import {HttpStatusCode} from '@angular/common/http';
import {AppService} from '../../../../services/app.service';
import {
  AppConfigurationServiceStub,
  AppServiceStub,
  DialogServiceStub,
  TranslateServiceStub,
} from '../../../../../test/stubs';
import {LogLevelDialogComponent} from "./partial/log-level-dialog/log-level-dialog.component";
import {AppConfigurationService} from "../../../../services/appConfiguration.service";
import {AppConfig} from "../../../../models/appConfig";

let instances: MessengerInstance[] = [
  {
    id: '1',
    userId: '1',
    serverName: 'AkquinetTestServer.matrix',
    publicBaseUrl: 'akquinet.de',
    version: 0,
    dateOfOrder: '15.03.2023',
    endDate: '15.03.2024,',
    active: true,
    startOfInactivity: 'test',
  },
  {
    id: '2',
    userId: '2',
    serverName: 'GematikTestServer.matrix',
    publicBaseUrl: 'gematik.de',
    version: 0,
    dateOfOrder: '15.03.2023',
    endDate: '15.03.2024,',
    active: true,
    startOfInactivity: 'test',
  },
];

let akquinetTestServer = 'AkquinetTestServer.matrix';

describe('MessengerInstancesListComponent', () => {
  let fixture: ComponentFixture<MessengerInstancesListComponent>;
  let component: MessengerInstancesListComponent;
  let dialogService: DialogService;
  let appConfigService: AppConfigurationService;
  let appService: AppService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        MessengerInstancesListComponent,
        DeleteMessengerInstancesDialogComponent,
        LogLevelDialogComponent
      ],
      imports: [
        ButtonModule,
        CommonModule,
        InputModule,
        DialogModule,
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        RouterModule,
        FormsModule,
        HttpClientTestingModule,
        LoadingIndicatorModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: DialogService, useClass: DialogServiceStub},
        {provide: AppService, useClass: AppServiceStub},
        {provide: AppConfigurationService, useClass: AppConfigurationServiceStub},
        {provide: TranslateService, useClass: TranslateServiceStub},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MessengerInstancesListComponent);
    component = fixture.componentInstance;
    dialogService = TestBed.inject(DialogService);
    appConfigService = TestBed.inject(AppConfigurationService);
    appService = TestBed.inject(AppService);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // run ngOnInit

    // mock instances GET request
    const getInstancesRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstance
    );
    expect(getInstancesRequest.request.method).toBe('GET');
    getInstancesRequest.flush(instances, {
      status: HttpStatusCode.Ok,
      statusText: 'Retrieved all instances for the user',
    });
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should retrieved instances in ngOnInit', () => {
    expect(component.messengerInstances).toEqual(instances);
  });

  it('should render instances list heading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#instances-list-heading')).toBeTruthy();
  });

  it('should render add instance button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#add-instance-button')?.textContent).toBe(
      ' ADMIN.INSTANCE_LIST.ADD '
    );
  });

  it('should render table', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('table > thead')).toBeTruthy();
    expect(compiled.querySelector('table > tbody')).toBeTruthy();

    expect(
      compiled.querySelector('table > thead > tr:first-child')?.textContent
    ).toContain('ADMIN.INSTANCE_LIST.TABLE_HEADS.SERVER_NAME');
    expect(
      compiled.querySelector('table > thead > tr:last-child')?.textContent
    ).toContain('ADMIN.INSTANCE_LIST.TABLE_HEADS.PUBLIC_BASE_URL');
  });

  it('should call openDeleteDialog and DialogService.openDialog()', () => {
    spyOn(component, 'openDeleteDialog').and.callThrough();
    spyOn(dialogService, 'openDialog');

    expect(component.openDeleteDialog).not.toHaveBeenCalled();
    expect(dialogService.openDialog).not.toHaveBeenCalled();

    fixture.detectChanges();

    fixture.debugElement
      .query(
        By.css('#delete-instance-button-' + akquinetTestServer.replace('.', ''))
      )
      .triggerEventHandler('click', null);

    expect(component.openDeleteDialog).toHaveBeenCalled();
    expect(dialogService.openDialog).toHaveBeenCalledWith(
      DeleteMessengerInstancesDialogComponent
    );
  });

  it('should call openLogDownloadDialog and DialogService.openDialog()', () => {
    spyOn(component, 'openLogDownloadDialog').and.callThrough();
    spyOn(dialogService, 'openDialog');

    expect(component.openLogDownloadDialog).not.toHaveBeenCalled();
    expect(dialogService.openDialog).not.toHaveBeenCalled();

    fixture.detectChanges();
    fixture.debugElement
      .query(
        By.css('#log-server-button-' + akquinetTestServer.replace('.', ''))
      )
      .triggerEventHandler('click', null);

    expect(component.openLogDownloadDialog).toHaveBeenCalled();
  });

  it('should delete instance with servername "AkquinetTestServer"', () => {
    component.deleteInstance(akquinetTestServer);

    const deleteRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl +
      ApiRoutes.messengerInstance +
      '/' +
      akquinetTestServer +
      '/'
    );
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(
      [
        {
          id: '1',
          userId: '1',
          serverName: 'AkquinetTestServer.matrix',
          publicBaseUrl: 'akquinet.de',
          version: 0,
          dateOfOrder: '15.03.2023',
          endDate: '15.03.2024,',
          active: true,
          startOfInactivity: 'test',
        },
      ],
      {
        status: HttpStatusCode.Ok,
        statusText: 'Ok',
      }
    );

    const getRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstance
    );
    expect(getRequest.request.method).toBe('GET');
    getRequest.flush(
      [
        {
          id: '2',
          userId: '2',
          serverName: 'GematikTestServer.matrix',
          publicBaseUrl: 'gematik.de',
          version: 0,
          dateOfOrder: '15.03.2023',
          endDate: '15.03.2024,',
          active: true,
          startOfInactivity: 'test',
        },
      ],
      {
        status: HttpStatusCode.Ok,
        statusText: 'Ok',
      }
    );

    expect(component.messengerInstances).toEqual([
      {
        id: '2',
        userId: '2',
        serverName: 'GematikTestServer.matrix',
        publicBaseUrl: 'gematik.de',
        version: 0,
        dateOfOrder: '15.03.2023',
        endDate: '15.03.2024,',
        active: true,
        startOfInactivity: 'test',
      },
    ]);
  });

  it('should return BadRequest on deleting instance', () => {
    spyOn(appService, 'showToast');
    component.deleteInstance('asdf&');

    const deleteRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstance + '/asdf&' + '/'
    );
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush('A input value contains wrong characters', {
      status: HttpStatusCode.BadRequest,
      statusText: 'BadRequest',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.BAD_REQUEST',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return NotFound on deleting instance', () => {
    spyOn(appService, 'showToast');
    component.deleteInstance('asdf');

    const deleteRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstance + '/asdf' + '/'
    );
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush('Messenger Instance not found', {
      status: HttpStatusCode.NotFound,
      statusText: 'NotFound',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.NOT_FOUND',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return InternalServerError with operatorError on deleting instance', () => {
    spyOn(appService, 'showToast');
    component.deleteInstance('asdf');

    const deleteRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstance + '/asdf' + '/'
    );
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(component.internalServerErrorDescription, {
      status: HttpStatusCode.InternalServerError,
      statusText: 'InternalServerError',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.INTERNAL_SERVER_ERROR',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return InternalServerError with operatorError on deleting instance', () => {
    spyOn(appService, 'showToast');
    component.deleteInstance('asdf');

    const deleteRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstance + '/asdf' + '/'
    );
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(
      {},
      {
        status: HttpStatusCode.InternalServerError,
        statusText: 'InternalServerError',
      }
    );

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.DELETE',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return Bad on logDownload no Servername', () => {
    spyOn(appService, 'showToast');
    component.logDownload('', 'synapse');

    const logRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceLogs + '//synapse'
    );
    expect(logRequest.request.method).toBe('GET');
    logRequest.flush(new Blob(), {
      status: HttpStatusCode.BadRequest,
      statusText: 'BadRequest',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD.ERROR.BAD_REQUEST',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return Not Found on logDownload Server not Found', () => {
    spyOn(appService, 'showToast');
    component.logDownload('asdf', 'synapse');

    const logRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceLogs + '/asdf/synapse'
    );
    expect(logRequest.request.method).toBe('GET');
    logRequest.flush(new Blob(), {
      status: HttpStatusCode.NotFound,
      statusText: 'NotFound',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD.ERROR.NOT_FOUND',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return InternalServerError on File Server Error', () => {
    spyOn(appService, 'showToast');
    component.logDownload('asdf', 'synapse');

    const logRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceLogs + '/asdf/synapse'
    );
    expect(logRequest.request.method).toBe('GET');
    logRequest.flush(new Blob(), {
      status: HttpStatusCode.InternalServerError,
      statusText: 'InternalServerError',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD.ERROR.INTERNAL_SERVER_ERROR',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should create admin user for servername "AkquinetTestServer"', () => {
    spyOn(appService, 'showToast');
    component.createAdmin(akquinetTestServer);

    const adminRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl +
      ApiRoutes.messengerInstance +
      '/' +
      akquinetTestServer +
      '/admin'
    );
    expect(adminRequest.request.method).toBe('GET');
    adminRequest.flush(
      {
        userName: '@testUser:akquinet.de',
        password: 'password',
      },
      {
        status: HttpStatusCode.Created,
        statusText: 'CREATED',
      }
    );
    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'Admin-Nutzer Erstellt:\nUsername:@testUser:akquinet.de\nPassword:password',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 0,
    });
  });

  it('should return not found if server was not found', () => {
    spyOn(appService, 'showToast');
    component.createAdmin(akquinetTestServer);

    const adminRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl +
      ApiRoutes.messengerInstance +
      '/' +
      akquinetTestServer +
      '/admin'
    );
    expect(adminRequest.request.method).toBe('GET');
    adminRequest.flush([{}], {
      status: HttpStatusCode.NotFound,
      statusText: 'NOT_FOUND',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.NOT_FOUND',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 0,
    });
  });

  it('should return Conflict if admin already exists', () => {
    spyOn(appService, 'showToast');
    component.createAdmin(akquinetTestServer);

    const adminRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl +
      ApiRoutes.messengerInstance +
      '/' +
      akquinetTestServer +
      '/admin'
    );
    expect(adminRequest.request.method).toBe('GET');
    adminRequest.flush([{}], {
      status: HttpStatusCode.Conflict,
      statusText: 'CONFLICT',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.CONFLICT',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 0,
    });
  });

it('should return Locked if instance is not ready', () => {
    spyOn(appService, 'showToast');
    component.createAdmin(akquinetTestServer);

    const adminRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl +
      ApiRoutes.messengerInstance +
      '/' +
      akquinetTestServer +
      '/admin'
    );
    expect(adminRequest.request.method).toBe('GET');
    adminRequest.flush([{}], {
      status: HttpStatusCode.Locked,
      statusText: 'CONFLICT',
    });

    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.LOCKED',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 0,
    });
  });

  it('should return INTERNAL_SERVER_ERROR if everything else fails', () => {
    spyOn(appService, 'showToast');
    component.createAdmin(akquinetTestServer);

    const adminRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl +
      ApiRoutes.messengerInstance +
      '/' +
      akquinetTestServer +
      '/admin'
    );
    expect(adminRequest.request.method).toBe('GET');
    adminRequest.flush([{}], {
      status: HttpStatusCode.InternalServerError,
      statusText: 'InternalServerError',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.INTERNAL_SERVER_ERROR',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 0,
    });
  });

  it('should create a new messenger instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const adminRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(adminRequest.request.method).toBe('POST');
    adminRequest.flush(
      {},
      {
        status: HttpStatusCode.Created,
        statusText: 'CREATED',
      }
    );
  });

  it('should return BAD_REQUEST on creating a new instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const createRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush([{}], {
      status: HttpStatusCode.BadRequest,
      statusText: 'BAD_REQUEST',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.BAD_REQUEST',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return NOT_FOUND on creating a new instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const createRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush([{}], {
      status: HttpStatusCode.NotFound,
      statusText: 'NOT_FOUND',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.NOT_FOUND',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return CONFLICT on creating a new instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const createRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush([{}], {
      status: HttpStatusCode.Conflict,
      statusText: 'CONFLICT',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.CONFLICT',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return PRECONDITION_FAILED on creating a new instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const createRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush([{}], {
      status: HttpStatusCode.PreconditionFailed,
      statusText: 'PRECONDITION_FAILED',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.PRECONDITION_FAILED',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return FORBIDDEN on creating a new instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const createRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush([{}], {
      status: HttpStatusCode.Forbidden,
      statusText: 'FORBIDDEN',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.FORBIDDEN',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return INTERNAL_SERVER_ERROR on creating a new instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const createRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush(component.internalServerErrorDescription, {
      status: HttpStatusCode.InternalServerError,
      statusText: 'InternalServerError',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.INTERNAL_SERVER_ERROR',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should return INTERNAL_SERVER_ERROR on creating a new instance', () => {
    spyOn(appService, 'showToast');
    component.createMessengerInstance();

    const createRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl + ApiRoutes.messengerInstanceCreate
    );
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush([{}], {
      status: HttpStatusCode.InternalServerError,
      statusText: 'INTERNAL_SERVER_ERROR',
    });
    expect(appService.showToast).toHaveBeenCalledWith({
      description: 'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.SAVE',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 5000,
    });
  });

  it('should set to first page (1)', () => {
    spyOn(component, 'firstPage').and.callThrough();
    component.currentPageNumber = 2;
    expect(component.firstPage()).toBe(1);
  });

  it('should set to previous page (2)', () => {
    spyOn(component, 'previousPage').and.callThrough();
    component.currentPageNumber = 3;
    expect(component.previousPage()).toBe(2);
  });

  it('should set to next page (2)', () => {
    spyOn(component, 'nextPage').and.callThrough();
    component.maxPageNumber = 3;
    component.currentPageNumber = 1;
    component.nextPage();
    expect(component.currentPageNumber).toBe(2);
  });

  it('should set to final page (3)', () => {
    spyOn(component, 'finalPage').and.callThrough();
    component.maxPageNumber = 3;
    component.finalPage();
    expect(component.currentPageNumber).toBe(3);
  });

  it('should return correct messenger instance', () => {
    spyOn(component, 'getPaginatedMessengerInstances').and.callThrough();
    component.messengerInstances = instances;
    component.currentPageNumber = 1;
    expect(component.getPaginatedMessengerInstances()).toEqual(instances);
  });

  it('should get correct page starting index (11)', () => {
    spyOn(component, 'getCurrentStartItemIndex').and.callThrough();
    component.currentPageNumber = 2;
    component.totalCount = 22;
    expect(component.getCurrentStartItemIndex()).toBe(11);
  });

  it('should get correct page end index (11)', () => {
    spyOn(component, 'getCurrentEndItemIndex').and.callThrough();
    component.currentPageNumber = 2;
    component.totalCount = 22;
    expect(component.getCurrentEndItemIndex()).toBe(20);
  });

  it('should calculate pagination numbers', () => {
    spyOn(component, 'calculatePaginationNumbers').and.callThrough();
    component.messengerInstances = instances;
    component.calculatePaginationNumbers();
    expect(component.maxPageNumber).toBe(1);
  });

  it('should call open and DialogService.openDialog()', () => {
    spyOn(component, 'openLogLevelChangeDialog').and.callThrough();
    spyOn(dialogService, 'openDialog');

    expect(component.openLogLevelChangeDialog).not.toHaveBeenCalled();
    expect(dialogService.openDialog).not.toHaveBeenCalled();

    fixture.detectChanges();

    fixture.debugElement
      .query(
        By.css('#log-change-button-' + akquinetTestServer.replace('.', ''))
      )
      .triggerEventHandler('click', null);

    expect(component.openLogLevelChangeDialog).toHaveBeenCalled();
    expect(dialogService.openDialog).toHaveBeenCalledWith(
      LogLevelDialogComponent
    );
  });

  it('should change loglevel with servername "AkquinetTestServer"', () => {
    component.changeLogLevel(akquinetTestServer);
    spyOn(appService, 'showToast');
    const postRequest = httpMock.expectOne(
      appConfigService.appConfig.apiUrl +
      ApiRoutes.messengerInstance +
      '/' +
      akquinetTestServer +
      '/loglevel'
    );
    expect(postRequest.request.method).toBe('POST');
    postRequest.flush(
      "DEBUG",
      {
        status: HttpStatusCode.Ok,
        statusText: 'Ok',
      }
    );
    expect(appService.showToast).toHaveBeenCalledWith({
      description:
        'ADMIN.INSTANCE_LIST.CHANGE_LOGLEVEL_DIALOG.SUCCESS',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 300000,
    });

  });

});
