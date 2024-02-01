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

import { Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { RestService } from '../../../../../../services/rest.service';
import ApiRoutes from '../../../../../../resources/api/api-routes';
import { saveAs } from 'file-saver';

import { DialogService } from '../../../../../../services/dialog.service';
import { HttpStatusCode } from '@angular/common/http';
import { AppService } from '../../../../../../services/app.service';

@Component({
  selector: 'log-download-event-dialog',
  templateUrl: './log-download-dialog.component.html',
  styleUrls: ['./log-download-dialog.component.scss'],
})
export class LogDownloadDialogComponent implements OnInit {
  public loading: boolean = false;
  public readonly internalServerErrorDescription = 'Error during log download.';
  public formGroup: FormGroup = new FormGroup({});

  errorKeys = {
    start: {
      required: 'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD_DIALOG.ERROR.START_REQUIRED',
    },
    timespan: {
      required:
        'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD_DIALOG.ERROR.TIMESPAN_REQUIRED',
      max: 'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD_DIALOG.ERROR.TIMESPAN_MAX',
      pattern: 'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD_DIALOG.ERROR.TIMESPAN_PATTERN',
    },
  };

  @Input()
  data: any;

  constructor(
    private restService: RestService,
    private dialogService: DialogService,
    private appService: AppService
  ) {}

  ngOnInit() {
    this.formGroup = new FormGroup({
      start: new FormControl(this.nowSomeDaysAgo(0), [Validators.required]),
      timespan: new FormControl('60', [
        Validators.required,
        Validators.pattern('^[0-9]{1,4}'),
        Validators.max(4320),
      ]),
    });
  }

  getError(controlName: string): string {
    const formControl = this.formGroup.get(controlName);
    if (!formControl?.touched || !formControl.errors) {
      return '';
    }
    const errors = this.errorKeys[controlName];
    for (let key of Object.keys(errors)) {
      if (formControl.errors[key]) {
        return errors[key];
      }
    }
    return '';
  }

  logDownload(): void {
    if (this.formGroup.valid) {
      this.loading = true;

      // Readd timezone information on the way back
      let startDate = new Date(this.formGroup.get('start')?.value + "Z");

      // prepare values for Loki request
      let start: number = startDate.getTime() / 1000;
      let end: number = start + Number(this.formGroup.get('timespan')?.value) * 60;

      this.restService
        .getFileFromApi(
          ApiRoutes.messengerInstanceLogs +
            '/' +
            this.data[0] +
            '/' +
            this.data[1] +
            `?start=${start}&end=${end}`
        )
        .subscribe({
          next: (response) => {
            this.loading = false;
            let filename = `${this.data[0]}_${
              this.data[1]
            }_log_${new Date()}.text`;
            let blob = new Blob([response], { type: 'text/plain' });
            saveAs(blob, filename);
          },
          error: (response) => {
            this.loading = false;
            this.dialogService.closeDialog(false);

            let errorDescription =
              'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD_DIALOG.ERROR.DOWNLOAD';

            if (
              response.status === HttpStatusCode.InternalServerError &&
              response.error === this.internalServerErrorDescription
            ) {
              errorDescription =
                'ADMIN.INSTANCE_LIST.LOG_DOWNLOAD_DIALOG.ERROR.INTERNAL_SERVER_ERROR';
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

  nowSomeDaysAgo(daysAgo: number): string {
    let now: Date = new Date();
    let past: Date = new Date(now.setDate(now.getDate() - daysAgo));

    // remove timezone information as it is not accepted by frontend component anyway
    return past.toISOString().slice(0, 16);
  }
}
