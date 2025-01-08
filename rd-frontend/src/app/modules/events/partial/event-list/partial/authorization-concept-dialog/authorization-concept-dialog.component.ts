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

import {Component, Input, OnInit} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';

import {DialogService} from '../../../../../../services/dialog.service';
import {HttpStatusCode} from '@angular/common/http';
import {AppService} from '../../../../../../services/app.service';
import {
  GetMessengerInstanceTimAuthConceptConfig200Response,
  GetMessengerInstanceTimAuthConceptConfig200ResponseFederationCheckConceptEnum,
  GetMessengerInstanceTimAuthConceptConfig200ResponseInviteRejectionPolicyEnum,
  MessengerInstanceService
} from "../../../../../../../../build/openapi/messengerinstance";
import {tap} from "rxjs";


@Component({
  selector: 'authorization-concept-dialog', templateUrl: './authorization-concept-dialog.component.html'
})
export class AuthorizationConceptDialogComponent implements OnInit {
  public loading: boolean = false;
  public formGroup: FormGroup = this.formBuilder.group({
    useOldAuthConcept: [true], useAllowAllAsDefault: [true],
  },)

  @Input() data: any;

  constructor(
      private dialogService: DialogService,
      private appService: AppService,
      private readonly formBuilder: FormBuilder,
      private readonly messengerInstanceService: MessengerInstanceService,
  ) {
  }

  ngOnInit() {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;
    this.messengerInstanceService.getMessengerInstanceTimAuthConceptConfig(this.data[0])

      .subscribe({
        next: (response) => {
          this.loading = false;
          this.formGroup.get('useOldAuthConcept')!.setValue(response.federationCheckConcept == GetMessengerInstanceTimAuthConceptConfig200ResponseFederationCheckConceptEnum.Proxy);
          this.formGroup.get('useAllowAllAsDefault')!.setValue(response.inviteRejectionPolicy == GetMessengerInstanceTimAuthConceptConfig200ResponseInviteRejectionPolicyEnum.AllowAll);
        }, error: (response) => {
          this.loading = false;
          this.dialogService.closeDialog();

          let errorDescription = 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.UNKNOWN_LOAD';

          if (response.status === HttpStatusCode.InternalServerError) {
            errorDescription = 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
          }

          this.appService.showToast({
            description: errorDescription, icon: 'fa-triangle-exclamation', iconColor: 'primary', timeout: 5000,
          });
        },
      });
  }

  saveSettings(): void {
    this.loading = true;


    const useOldAuthConcept = this.formGroup.get('useOldAuthConcept')!.value;
    const useAllowAllAsDefault = this.formGroup.get('useAllowAllAsDefault')!.value;

    const federationCheckConcept = useOldAuthConcept
      ? GetMessengerInstanceTimAuthConceptConfig200ResponseFederationCheckConceptEnum.Proxy
      : GetMessengerInstanceTimAuthConceptConfig200ResponseFederationCheckConceptEnum.Client;


    const inviteRejectionPolicy = useAllowAllAsDefault
      ? GetMessengerInstanceTimAuthConceptConfig200ResponseInviteRejectionPolicyEnum.AllowAll
      : GetMessengerInstanceTimAuthConceptConfig200ResponseInviteRejectionPolicyEnum.BlockAll;

    const requestBody: GetMessengerInstanceTimAuthConceptConfig200Response = {
      federationCheckConcept: federationCheckConcept,
      inviteRejectionPolicy: inviteRejectionPolicy
    }

    this.messengerInstanceService.putMessengerInstanceTimAuthConceptConfig(this.data[0], requestBody)

      .pipe(tap((response: string) => {
        this.dialogService.closeDialog(response);
        this.appService.showToast({
          description: 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.CONFIG_SAVED',
          icon: 'fa-triangle-exclamation',
          iconColor: 'primary',
          timeout: 5000,
        });
        this.loading = false;
      })).subscribe({
      error: (response) => {
        let errorDescription = 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.UNKNOWN'

        if (response.status === HttpStatusCode.BadRequest) {
          errorDescription = 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.BAD_REQUEST';
        } else if (response.status === HttpStatusCode.NotFound) {
          errorDescription = 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.NOT_FOUND';
        } else if (response.status === HttpStatusCode.InternalServerError) {
          errorDescription = 'ADMIN.INSTANCE_LIST.AUTH_CONCEPT_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
        }

        this.appService.showToast({
          description: errorDescription, icon: 'fa-triangle-exclamation', iconColor: 'primary', timeout: 5000,
        });

        this.loading = false;
      },
    });
  }
}
