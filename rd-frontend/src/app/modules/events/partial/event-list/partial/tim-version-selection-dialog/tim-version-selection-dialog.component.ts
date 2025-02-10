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

import {Component} from '@angular/core';
import {FormBuilder, FormGroup} from "@angular/forms";

import {DialogService} from '../../../../../../services/dialog.service';
import {timVariantOptions} from './tim-variant-options';


const timVariantSelector = 'timVariantSelector'


@Component({
  selector: 'tim-version-selection-dialog', templateUrl: './tim-version-selection-dialog.component.html',
})
export class TimVersionSelectionDialogComponent {
  public loading: boolean = false;
  public formGroup: FormGroup = this.formBuilder.group({
    timVariantSelector: timVariantOptions.pro
  },)
  public timVariantOptions = timVariantOptions;

  constructor(private dialogService: DialogService, private readonly formBuilder: FormBuilder,) {
  }

  closeDialog(): void {
    this.dialogService.closeDialog({createInstance: false});
  }


  requestInstance(): void {
    this.dialogService.closeDialog({
      createInstance: true, timVersion: this.formGroup.get(timVariantSelector)!.value
    });
  }
}
