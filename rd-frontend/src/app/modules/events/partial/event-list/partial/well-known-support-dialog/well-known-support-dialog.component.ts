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

import {HttpStatusCode} from "@angular/common/http";
import {Component, Input, OnInit} from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";

import {Contact, ContactRole, ServerSupportInformation, WellKnownSupportService} from 'build/openapi/wellknownsupport';
import {enumValuesToSelectOptions} from "../../../../../../components/select/model/select-option";
import {AppService} from "../../../../../../services/app.service";
import {DialogService} from "../../../../../../services/dialog.service";

@Component({
  selector: 'app-well-known-support-dialog',
  templateUrl: './well-known-support-dialog.component.html'
})
export class WellKnownSupportDialogComponent implements OnInit {
  public isLoading: boolean = false;
  public form: FormGroup = this.formBuilder.group(
    {
      page: "",
      contacts: this.formBuilder.array([])
    },
    { validators: atLeastOneFieldRequired('page', 'contacts') }
  );

  @Input() data: any;

  constructor(
    private dialogService: DialogService,
    private appService: AppService,
    private readonly formBuilder: FormBuilder,
    private readonly wellKnownSupportService: WellKnownSupportService
  ) {
  }

  ngOnInit() {
    this.loadSettings();
  }

  loadSettings() {

    this.isLoading = true;

    this.wellKnownSupportService.retrieveSupportInformation(this.data[0])
      .subscribe({
        next: (response) => {
          this.isLoading = false;

          this.form.setValue({
            page: response.support_page ?? "",
            contacts: []
          });

          response.contacts.forEach(
            (it: Contact) => this.addContact(it)
          );

        }, error: (response) => {
          this.isLoading = false;

          if(response.status === HttpStatusCode.NotFound){
            return;
          }

          let errorDescription = 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.UNKNOWN';

          if (response.status === HttpStatusCode.InternalServerError) {
            errorDescription = 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
          }

          this.appService.showToast({
            description: errorDescription, icon: 'fa-triangle-exclamation', iconColor: 'primary', timeout: 5000,
          });

          this.dialogService.closeDialog();
        },
      });
  }

  onSubmit() {
    if(this.form.invalid){
      return
    }

    this.isLoading = true;
    const supportInfo: ServerSupportInformation = this.formValueToServerSupportInformation()

    this.wellKnownSupportService.setSupportInformation(
      this.data[0],
      supportInfo,
    ).subscribe({
      next:() => {
        this.isLoading = false
        this.appService.showToast({
          description: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.SUCCESS',
          icon: 'fa-check',
          iconColor: 'green',
          timeout: 5000,
        });
        this.dialogService.closeDialog();
      },
      error: (response) => {
        this.isLoading = false

        let errorDescription: string = this.responseErrorMessages[response.status]
          ?? 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.UNKNOWN';

        this.appService.showToast({
          description: errorDescription, icon: 'fa-triangle-exclamation', iconColor: 'primary', timeout: 5000,
        });
      }
    })
  }

  private formValueToServerSupportInformation(): ServerSupportInformation {
    const value = this.form.value;
    const supportPage = value.page;
    const contactArray: Contact[] = value.contacts.map(
      (it: { email: string | null; mxId: string | null; role: ContactRole; }): Contact => {
        return {
          email_address: it.email,
          matrix_id: it.mxId,
          role: it.role
        };
      }
    );

    return {
      support_page: supportPage,
      contacts: contactArray
    }
  }

  get contacts() {
    return this.form.get('contacts') as FormArray;
  }

  addContact(contact: Contact | null = null) {
    this.contacts.push(
      this.formBuilder.group(
        {
          email: new FormControl(contact?.email_address || "", Validators.email),
          mxId: new FormControl(contact?.matrix_id || "", [
            Validators.minLength(1),
            Validators.maxLength(255),
            Validators.pattern("@[a-z0-9\\.\\_\\=\\-\\/\\+]+:(\\w|\\d)+(:\\d{1,5})?")
          ]),
          role: contact?.role || ContactRole.Admin
        },
        {
          validators: atLeastOneFieldRequired('email', 'mxId')
        }
      )
    )
  }

  removeContact(index: number) {
    this.contacts.removeAt(index);
  }

  protected readonly contactRoles = enumValuesToSelectOptions(ContactRole);

  responseErrorMessages = {
    [HttpStatusCode.BadRequest]: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.BAD_REQUEST',
    [HttpStatusCode.Unauthorized]: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.UNAUTHORIZED',
    [HttpStatusCode.Forbidden]: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.FORBIDDEN',
    [HttpStatusCode.InternalServerError]: 'ADMIN.INSTANCE_LIST.WELLKNOWN_SUPPORT_INFO_DIALOG.ERROR.INTERNAL_SERVER_ERROR'
  }
}

function atLeastOneFieldRequired(field1: string, field2: string): ValidatorFn {
  return (formGroup: AbstractControl): ValidationErrors | null => {
    const group = formGroup as FormGroup;
    return (
      Validators.required(group.controls[field1]) == null ||
      Validators.required(group.controls[field2]) == null
    )
      ? null
      : { atLeastOneRequired: true };
  };
}
