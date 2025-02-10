/*
 * Copyright (C) 2023-2025 akquinet GmbH
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

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import de from './assets/i18n/de.json';
import {I18nService} from '../../../../services/i18n.service';
import {RestService} from '../../../../services/rest.service';
import ApiRoutes from '../../../../resources/api/api-routes';
import {AppService} from '../../../../services/app.service';
import {DialogService} from '../../../../services/dialog.service';
import {
    DeleteMessengerInstancesDialogComponent
} from './partial/delete-messenger-instance-dialog/delete-messenger-instances-dialog.component';
import {tap} from 'rxjs';
import {HttpHeaders, HttpStatusCode} from '@angular/common/http';
import {saveAs} from 'file-saver';
import {AdminCreatedDialogComponent} from './partial/admin-created-dialog/admin-created-dialog.component';
import {LogLevelDialogComponent} from './partial/log-level-dialog/log-level-dialog.component';
import {LogDownloadDialogComponent} from './partial/log-download-dialog/log-download-dialog.component';
import {AppConfigurationService} from "../../../../services/appConfiguration.service";
import {
  AuthorizationConceptDialogComponent
} from "./partial/authorization-concept-dialog/authorization-concept-dialog.component";
import {
  CreateAdminUser201Response,
  MessengerInstanceDto,
  MessengerInstanceService
} from "../../../../../../build/openapi/messengerinstance";
import {LoggingService} from "../../../../../../build/openapi/logging";
import {
  TimVersionSelectionDialogComponent
} from "./partial/tim-version-selection-dialog/tim-version-selection-dialog.component";
import {timVariantOptions, TimVariantRef} from "./partial/tim-version-selection-dialog/tim-variant-options";

@Component({
    selector: 'admin-messenger-service-list',
    templateUrl: './messenger-instances-list.component.html',
    styleUrls: ['./messenger-instances-list.component.scss'],
})
export class MessengerInstancesListComponent implements OnInit {
    public messengerInstances: MessengerInstanceDto[] = [];
    public totalCount: number = 0;
    public currentPageNumber: number = 1;
    public maxPageNumber: number = 0;
    public itemsPerPage: number = 10;
    public isLoading: boolean = true;
    public isLoadingCreateInstance: boolean = false;
    public readonly internalServerErrorDescription =
        'Error on deleting new instance through operator';

    constructor(
        private i18nService: I18nService,
        private activatedRoute: ActivatedRoute,
        private appService: AppService,
        private dialogService: DialogService,
        private readonly appConfigService: AppConfigurationService,
        private readonly messengerInstanceService: MessengerInstanceService,
        private readonly loggingService: LoggingService

    ) {
    }

    ngOnInit(): void {
        this.i18nService.addTranslation('de', de);
        this.loadMessengerInstances();
    }

    loadMessengerInstances(): void {
        this.isLoading = true;
      this.messengerInstanceService.getMessengerInstances()
        .pipe(
          tap((messengerInstances) => {
            this.messengerInstances = messengerInstances;
            this.calculatePaginationNumbers();
            this.isLoading = false;
          })
        )
            .subscribe({
                error: () => {
                    this.messengerInstances = [];
                    this.calculatePaginationNumbers();
                    this.isLoading = false;
                    this.appService.showToast({
                        icon: 'fa-triangle-exclamation',
                        iconColor: 'primary',
                        description: 'ERROR.GENERIC_LOADING',
                        timeout: 5000,
                    });
                },
            });
    }

    getPaginatedMessengerInstances(): MessengerInstanceDto[] {
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
      this.loggingService.getLogs(serverName, component)
        .subscribe({
          next: (response) => {
            let filename = `${serverName}_log_${new Date()}.text`;
            let blob = new Blob([response], {type: 'text/plain'});
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
            this.appConfigService.appConfig.keycloakUrl +
            '/admin/' +
            serverName.replace(/\./g, '') +
            '/console';

        window.open(url, '_blank');
    }

    deleteInstance(serverName: string) {
        this.isLoading = true;
      this.messengerInstanceService
        .deleteMessengerService(serverName)
        .pipe(tap(() => this.loadMessengerInstances()))
        .subscribe({
          error: (response) => {
            this.isLoading = false;

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
        this.isLoading = true;
      this.messengerInstanceService.createAdminUser({instanceName: serverName})
        .pipe(
          tap((adminUser) => {
            let responseDescription =
              'Admin-Nutzer Erstellt:' +
              '\nUsername:' + adminUser.username +
              '\nPassword:' + adminUser.password;
            this.appService.showToast({
              description: responseDescription,
              icon: 'fa-triangle-exclamation',
              iconColor: 'primary',
              timeout: 0,
            });
            this.isLoading = false;
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
                    this.isLoading = false;

                    this.appService.showToast({
                        description: responseDescription,
                        icon: 'fa-triangle-exclamation',
                        iconColor: 'primary',
                        timeout: 0,
                    });
                },
            });
    }

    openAdminCreatedDialog(adminUser: CreateAdminUser201Response): void {
        this.dialogService.openDialog(AdminCreatedDialogComponent, {
            closeOnOutsideClick: true,
            showCloseButton: true,
            data: adminUser,
        });
    }

  openAuthConceptDialog(serverName: string) {
    this.dialogService.openDialog(AuthorizationConceptDialogComponent,{
      closeOnOutsideClick: true,
      showCloseButton: true,
      data: [serverName],
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
      this.loggingService.changeInstanceLogLevel(serverName, 'DEBUG')
        .pipe(
          tap(() => {
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
            this.isLoading = false;

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

    requestMessengerInstance(): void {

      this.dialogService.openDialog(TimVersionSelectionDialogComponent, {
        closeOnOutsideClick: true,
        showCloseButton: true,
      });

      this.dialogService.dialogClose$.subscribe((dialogResponse) => {
        if (dialogResponse == undefined){
          this.isLoadingCreateInstance = false;
          return;
        }
        if (dialogResponse.createInstance == true) {
          this.createMessengerInstance(dialogResponse.timVersion);
        }
      });
    }


    createMessengerInstance(variant: String){

      let variantEnumString: TimVariantRef = this.extractTimVariantEnumStringFromVariantSelection(variant);


      this.isLoadingCreateInstance = true;
      this.messengerInstanceService.requestMessengerInstance(variantEnumString)
        .pipe(
          tap((response: string) => {
            this.dialogService.closeDialog(response);
            this.appService.showToast({
              description: 'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ACCEPTED',
              icon: 'fa-triangle-exclamation',
              iconColor: 'primary',
              timeout: 5000,
            });
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
            } else if (response.status === HttpStatusCode.PaymentRequired) {
              errorDescription =
                'ADMIN.INSTANCE_LIST.CREATE_INSTANCE_DIALOG.ERROR.PAYMENT_REQUIRED';
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

  extractTimVariantEnumStringFromVariantSelection(variant: String) {
    let variantEnumString: TimVariantRef;
    switch (variant) {
      case timVariantOptions.classic:
        variantEnumString = "ref_1";
        break;
      case timVariantOptions.pro:
        variantEnumString = "ref_2";
        break;
      default:
        throw Error('Ungültige Tim Variante ausgewählt');
    }
    return variantEnumString;
  }

}

