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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import de from './assets/i18n/de.json';
import { I18nService } from '../../../../services/i18n.service';
import { RestService } from '../../../../services/rest.service';
import ApiRoutes from '../../../../resources/api/api-routes';
import { AppService } from '../../../../services/app.service';
import { MessengerInstance } from '../../../../models/messengerInstance';
import { DialogService } from '../../../../services/dialog.service';
import { DeleteMessengerInstancesDialogComponent } from './partial/delete-messenger-instance-dialog/delete-messenger-instances-dialog.component';
import { tap } from 'rxjs';
import { HttpStatusCode } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { AdminUser } from '../../../../models/adminUser';
import { AdminCreatedDialogComponent } from './partial/admin-created-dialog/admin-created-dialog.component';
import { LogLevelDialogComponent } from './partial/log-level-dialog/log-level-dialog.component';
import { LogDownloadDialogComponent } from './partial/log-download-dialog/log-download-dialog.component';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'admin-messenger-service-list',
  templateUrl: './messenger-instances-list.component.html',
  styleUrls: ['./messenger-instances-list.component.scss'],
})
export class MessengerInstancesListComponent implements OnInit {
  public messengerInstances: MessengerInstance[] = [];
  public totalCount: number = 0;
  public currentPageNumber: number = 1;
  public maxPageNumber: number = 0;
  public itemsPerPage: number = 10;
  public loading: boolean = true;
  public readonly internalServerErrorDescription =
    'Error on deleting new instance through operator';

  constructor(
    private i18nService: I18nService,
    private activatedRoute: ActivatedRoute,
    private restService: RestService,
    private appService: AppService,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {
    this.i18nService.addTranslation('de', de);
    this.loadMessengerInstances();
  }

  loadMessengerInstances(): void {
    this.loading = true;
    this.restService
      .getFromApi<MessengerInstance[]>(ApiRoutes.messengerInstance)
      .pipe(
        tap((messengerInstances) => {
          this.messengerInstances = messengerInstances;
          this.calculatePaginationNumbers();
          this.loading = false;
        })
      )
      .subscribe({
        error: () => {
          this.messengerInstances = [];
          this.calculatePaginationNumbers();
          this.loading = false;
          this.appService.showToast({
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            description: 'ERROR.GENERIC_LOADING',
            timeout: 5000,
          });
        },
      });
  }

  getPaginatedMessengerInstances(): MessengerInstance[] {
    return this.messengerInstances.slice(
      (this.currentPageNumber - 1) * this.itemsPerPage,
      this.currentPageNumber * this.itemsPerPage
    );
  }

  getCurrentStartItemIndex(): number {
    return Math.min(
      (this.currentPageNumber - 1) * this.itemsPerPage + 1,
      this.totalCount
    );
  }

  getCurrentEndItemIndex(): number {
    return Math.min(
      this.currentPageNumber * this.itemsPerPage,
      this.totalCount
    );
  }

  calculatePaginationNumbers(): void {
    this.totalCount = this.messengerInstances.length;
    this.maxPageNumber =
      this.totalCount === 0
        ? 1
        : Math.ceil(this.totalCount / this.itemsPerPage);
    this.currentPageNumber = Math.min(
      this.currentPageNumber,
      this.maxPageNumber
    );
  }

  firstPage(): number {
    this.currentPageNumber = 1;
    return this.currentPageNumber;
  }

  previousPage(): number {
    this.currentPageNumber = Math.max(this.currentPageNumber - 1, 1);
    return this.currentPageNumber;
  }

  nextPage(): void {
    this.currentPageNumber = Math.min(
      this.currentPageNumber + 1,
      this.maxPageNumber
    );
  }

  finalPage(): void {
    this.currentPageNumber = this.maxPageNumber;
  }

  logDownload(serverName: string, component: string) {
    this.restService
      .getFileFromApi(
        ApiRoutes.messengerInstanceLogs + '/' + serverName + '/' + component
      )
      .subscribe({
        next: (response) => {
          let filename = `${serverName}_log_${new Date()}.text`;
          let blob = new Blob([response], { type: 'text/plain' });
          saveAs(blob, filename);
        },
        error: (response) => {
          let errorDescription =
            'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD.ERROR.UNKNOWN';

          if (response.status === HttpStatusCode.BadRequest) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD.ERROR.BAD_REQUEST';
          } else if (response.status === HttpStatusCode.NotFound) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD.ERROR.NOT_FOUND';
          } else if (response.status === HttpStatusCode.InternalServerError) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD.ERROR.INTERNAL_SERVER_ERROR';
          }

          this.appService.showToast({
            description: errorDescription,
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            timeout: 5000,
          });
        },
      });
  }

  openDeleteDialog(clickEvent: MouseEvent, serverName: string): void {
    this.dialogService.openDialog(DeleteMessengerInstancesDialogComponent);
    this.dialogService.dialogClose$.subscribe((dialogResponse) => {
      if (dialogResponse.deleteMessengerInstance) {
        this.deleteInstance(serverName);
      }
    });
  }

  openKeycloakRealmConsole(serverName: string): void {
    // TODO better approach would be to fetch the home URL from the keycloak client directly
    const url =
      environment.keycloakUrl +
      '/admin/' +
      serverName.replace(/\./g, '') +
      '/console';

    window.open(url, '_blank');
  }

  deleteInstance(serverName: string) {
    this.loading = true;
    this.restService
      .deleteFromApi(ApiRoutes.messengerInstance + '/' + serverName + '/')
      .pipe(tap(() => this.loadMessengerInstances()))
      .subscribe({
        error: (response) => {
          this.loading = false;

          let errorDescription =
            'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.DELETE';

          if (response.status === HttpStatusCode.BadRequest) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.BAD_REQUEST';
          } else if (response.status === HttpStatusCode.NotFound) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.NOT_FOUND';
          } else if (
            response.status === HttpStatusCode.InternalServerError &&
            response.error === this.internalServerErrorDescription
          ) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.DELETE_INSTANCE_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
          }

          this.appService.showToast({
            description: errorDescription,
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            timeout: 5000,
          });
        },
      });
  }

  createAdmin(serverName: string) {
    this.loading = true;
    this.restService
      .getFromApi<AdminUser>(
        ApiRoutes.messengerInstance + '/' + serverName + '/admin'
      )
      .pipe(
        tap((adminUser) => {
          let responseDescription =
            'Admin-Nutzer Erstellt:' +
            '\nUsername:' +
            adminUser.userName +
            '\nPassword:' +
            adminUser.password;
          this.appService.showToast({
            description: responseDescription,
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            timeout: 0,
          });
          this.loading = false;
          this.openAdminCreatedDialog(adminUser);
        })
      )
      .subscribe({
        error: (response) => {
          let responseDescription =
            'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.SUCCESS';
          if (response.status === HttpStatusCode.NotFound) {
            responseDescription =
              'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.NOT_FOUND';
          } else if (response.status === HttpStatusCode.Conflict) {
            responseDescription =
              'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.CONFLICT';
          } else if (response.status === HttpStatusCode.Locked) {
            responseDescription =
              'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.LOCKED';
          } else if (response.status === HttpStatusCode.InternalServerError) {
            responseDescription =
              'ADMIN.INSTANCE_LIST.CREATE_ADMIN_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
          }
          this.loading = false;

          this.appService.showToast({
            description: responseDescription,
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            timeout: 0,
          });
        },
      });
  }

  openAdminCreatedDialog(adminUser: AdminUser): void {
    this.dialogService.openDialog(AdminCreatedDialogComponent, {
      closeOnOutsideClick: true,
      showCloseButton: true,
      data: adminUser,
    });
  }

  openLogLevelChangeDialog(serverName: string) {
    this.dialogService.openDialog(LogLevelDialogComponent);
    this.dialogService.dialogClose$.subscribe((dialogResponse) => {
      if (dialogResponse.changeLogLevel) {
        this.changeLogLevel(serverName);
      }
    });
  }

  changeLogLevel(serverName: string) {
    this.restService
      .postToApi<string>(
        'DEBUG',
        ApiRoutes.messengerInstance + '/' + serverName + '/loglevel'
      )
      .pipe(
        tap((adminUser) => {
          let responseDescription =
            'ADMIN.INSTANCE_LIST.CHANGE_LOGLEVEL_DIALOG.SUCCESS';
          this.appService.showToast({
            description: responseDescription,
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            timeout: 300000,
          });
        })
      )
      .subscribe({
        error: (response) => {
          this.loading = false;

          let responseDescription =
            'ADMIN.INSTANCE_LIST.CHANGE_LOGLEVEL_DIALOG.ERROR.UNKNOWN';

          if (response.status === HttpStatusCode.NotFound) {
            responseDescription =
              'ADMIN.INSTANCE_LIST.CHANGE_LOGLEVEL_DIALOG.ERROR.NOT_FOUND';
          } else if (
            response.status === HttpStatusCode.InternalServerError &&
            response.error === this.internalServerErrorDescription
          ) {
            responseDescription =
              'ADMIN.INSTANCE_LIST.CHANGE_LOGLEVEL_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
          }

          this.appService.showToast({
            description: responseDescription,
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            timeout: 5000,
          });
        },
      });
  }

  openLogDownloadDialog(serverName: string, component: string): void {
    this.dialogService.openDialog(LogDownloadDialogComponent, {
      closeOnOutsideClick: true,
      showCloseButton: true,
      data: [serverName, component],
    });
  }

  createMessengerInstance(): void {
    this.restService
      .postToApi<string>('', ApiRoutes.messengerInstanceCreate)
      .pipe(
        tap((response: string) => {
          this.dialogService.closeDialog(response);
          this.messengerInstances = [];
          this.loadMessengerInstances();
        })
      )
      .subscribe({
        error: (response) => {
          let errorDescription =
            'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.SAVE';

          if (response.status === HttpStatusCode.BadRequest) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.BAD_REQUEST';
          } else if (response.status === HttpStatusCode.NotFound) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.NOT_FOUND';
          } else if (response.status === HttpStatusCode.Conflict) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.CONFLICT';
          } else if (response.status === HttpStatusCode.PreconditionFailed) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.PRECONDITION_FAILED';
          } else if (response.status === HttpStatusCode.Forbidden) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.FORBIDDEN';
          } else if (
            response.status === HttpStatusCode.InternalServerError &&
            response.error === this.internalServerErrorDescription
          ) {
            errorDescription =
              'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
          }

          this.appService.showToast({
            description: errorDescription,
            icon: 'fa-triangle-exclamation',
            iconColor: 'primary',
            timeout: 5000,
          });
        },
      });
  }
}
