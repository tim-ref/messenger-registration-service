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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { LoadingIndicatorModule } from '../../components/loading-indicator/loading-indicator.module';
import { MessengerInstanceRouting } from './messenger-instance.routing';
import { MessengerInstanceComponent } from './messenger-instance.component';
import { MessengerInstancesListComponent } from './partial/event-list/messenger-instances-list.component';
import { DeleteMessengerInstancesDialogComponent } from './partial/event-list/partial/delete-messenger-instance-dialog/delete-messenger-instances-dialog.component';
import { ButtonModule } from '../../components/button/button.module';
import { InputModule } from '../../components/input/input.module';
import { DialogModule } from '../../components/dialog/dialog.module';
import { SelectModule } from "../../components/select/select.module";
import {AdminCreatedDialogComponent} from "./partial/event-list/partial/admin-created-dialog/admin-created-dialog.component";
import {LogDownloadDialogComponent} from "./partial/event-list/partial/log-download-dialog/log-download-dialog.component";
import {LogLevelDialogComponent} from "./partial/event-list/partial/log-level-dialog/log-level-dialog.component";
import {
  AuthorizationConceptDialogComponent
} from "./partial/event-list/partial/authorization-concept-dialog/authorization-concept-dialog.component";
import {
  TimVersionSelectionDialogComponent
} from "./partial/event-list/partial/tim-version-selection-dialog/tim-version-selection-dialog.component";
import {
  WellKnownSupportDialogComponent
} from "./partial/event-list/partial/well-known-support-dialog/well-known-support-dialog.component";

@NgModule({
  declarations: [
    MessengerInstanceComponent,
    MessengerInstancesListComponent,
    DeleteMessengerInstancesDialogComponent,
    LogLevelDialogComponent,
    AdminCreatedDialogComponent,
    LogDownloadDialogComponent,
    AuthorizationConceptDialogComponent,
    TimVersionSelectionDialogComponent,
    WellKnownSupportDialogComponent
  ],
  imports: [
    ButtonModule,
    CommonModule,
    InputModule,
    SelectModule,
    DialogModule,
    RouterModule.forChild(MessengerInstanceRouting),
    TranslateModule,
    RouterModule,
    FormsModule,
    LoadingIndicatorModule,
    ReactiveFormsModule,
  ],
  exports: [MessengerInstanceComponent, WellKnownSupportDialogComponent],
})
export class MessengerInstanceModule {}
