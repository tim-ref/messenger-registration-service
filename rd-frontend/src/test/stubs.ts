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

import { Observable, of } from 'rxjs';
import { Toast } from '../app/components/toast/model/toast';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {AppConfig} from "../app/models/appConfig";

export class DialogServiceStub {
  public dialogClose$: Observable<any> = new Observable<any>();
  public openDialog(): void {}
  public closeDialog(data?: any): void {}
}

export class AppServiceStub {
  public showToast(toastConfig: Toast): void {}
}

export class AppConfigurationServiceStub {
  appConfig: AppConfig = new AppConfig();
}

@Injectable({
  providedIn: 'root',
})
export class TranslateServiceStub extends TranslateService {
  public override get(key: any): any {
    return of(key);
  }
}
