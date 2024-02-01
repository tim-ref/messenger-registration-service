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

import { TestBed } from '@angular/core/testing';
import { DialogService } from '../dialog.service';
import {AdminCreatedDialogComponent} from "../../modules/events/partial/event-list/partial/admin-created-dialog/admin-created-dialog.component";

let dialogService: DialogService

describe('DialogService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [DialogService] });
    dialogService = TestBed.inject(DialogService)
  });

  it('should open open dialog observable', (done: DoneFn) => {
    dialogService.dialog$.subscribe((value) => {
      expect(value).toEqual({
        component: AdminCreatedDialogComponent,
        options: {
          closeOnOutsideClick: true,
          showCloseButton: false,
          data: {},
        },
      });
      done();
    });

    dialogService.openDialog(AdminCreatedDialogComponent, {
      closeOnOutsideClick: true,
      showCloseButton: false,
      data: {},
    });
  });

  it('should call close observable', (done: DoneFn) => {
    dialogService.openDialog(AdminCreatedDialogComponent, {
      closeOnOutsideClick: true,
      showCloseButton: false,
      data: {},
    });

    dialogService.dialogClose$.subscribe((value) => {
      expect(value).toBeTruthy();
      done();
    });

    dialogService.closeDialog(1);
  });

})
