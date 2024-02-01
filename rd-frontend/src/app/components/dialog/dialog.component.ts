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

import { Component, OnDestroy, OnInit, Type, ViewChild } from '@angular/core';
import { DialogService } from '../../services/dialog.service';
import { Subscription, tap } from 'rxjs';
import { HostDirective } from '../../directives/host.directive';
import { DialogOptions } from './model/dialog-options';
import de from './assets/i18n/de.json';
import { I18nService } from '../../services/i18n.service';

@Component({
  selector: 'generic-dialog',
  templateUrl: './dialog.component.html',
  styleUrls: ['./dialog.component.scss'],
})
export class DialogComponent implements OnInit, OnDestroy {
  @ViewChild(HostDirective, { static: true })
  // @ts-ignore
  private componentHost: HostDirective;
  private dialogServiceSubscription: Subscription = new Subscription();
  public options: DialogOptions = new DialogOptions();
  public isDialogOpen: boolean = false;

  constructor(
    private i18nService: I18nService,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {
    this.i18nService.addTranslation('de', de);
    this.dialogServiceSubscription = this.dialogService.dialog$
      .pipe(
        tap((dialog: { component: Type<any>; options: DialogOptions }) => {
          this.componentHost.viewContainerRef.clear();
          const componentRef =
            this.componentHost.viewContainerRef.createComponent<any>(
              dialog.component
            );
          componentRef.instance.data = dialog.options.data;
          this.isDialogOpen = true;
          this.options = dialog.options;

          this.dialogService.dialogClose$
            .pipe(
              tap(() => {
                this.closeDialog();
              })
            )
            .subscribe();
        })
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.dialogServiceSubscription.unsubscribe();
  }

  closeDialog(): void {
    this.componentHost.viewContainerRef.clear();
    this.isDialogOpen = false;
  }

  backdropClick(): void {
    if (this.options?.closeOnOutsideClick) {
      this.initDialogClose();
    }
  }

  initDialogClose(): void {
    this.dialogService.closeDialog();
  }
}
