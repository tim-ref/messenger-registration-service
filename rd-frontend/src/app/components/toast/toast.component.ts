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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription, tap } from 'rxjs';
import { Toast } from './model/toast';
import { AppService } from '../../services/app.service';

@Component({
  selector: 'generic-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss'],
})
export class ToastComponent implements OnInit, OnDestroy {
  showToast: boolean = false;
  subscription: Subscription = new Subscription();

  toastConfig: Toast = new Toast();
  // set type to any because type Timeout has no default parameter
  public timeout: any;

  constructor(private appService: AppService) {}

  ngOnInit(): void {
    this.subscription = this.appService.toast$
      .pipe(
        tap((toastConfig: Toast) => {
          this.toastConfig = toastConfig;
          this.showToast = true;
          clearTimeout(this.timeout);
          if (toastConfig.timeout != null && toastConfig.timeout > 0) {
            this.timeout = setTimeout(
              () => this.hideToast(),
              toastConfig.timeout
            );
          }
        })
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  hideToast(event?): void {
    if (this.toastConfig.onClose) {
      this.toastConfig.onClose();
    }
    clearTimeout(this.timeout);
    this.showToast = false;
    // suppress click event so that onClick is not fired
    if (event) {
      event.stopPropagation();
    }
  }

  onClick(): void {
    if (this.toastConfig.onClick) {
      this.toastConfig.onClick();
    }
  }
}
