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

import { Injectable, Type } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DialogOptions } from '../components/dialog/model/dialog-options';

@Injectable({
  providedIn: 'root',
})
export class DialogService {
  private dialogSource = new Subject();
  public dialog$: Observable<any> = this.dialogSource.asObservable();

  private dialogCloseSource: Subject<any> = new Subject<any>();
  public dialogClose$: Observable<any> = new Observable<any>();

  public openDialog(component: Type<any>, options?: DialogOptions): void {
    if (options == null) {
      options = new DialogOptions();
    }
    this.dialogCloseSource = new Subject<any>();
    this.dialogClose$ = this.dialogCloseSource.asObservable();
    this.dialogSource.next({ component, options });
  }

  public closeDialog(data?: any): void {
    this.dialogCloseSource.next(data);
    this.dialogCloseSource.complete();
  }
}
