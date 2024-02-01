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

import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { MessengerInstanceModule } from './modules/events/messenger-instance.module';
import { ToolbarComponent } from './components/toolbar/toolbar.component';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { TimeoutInterceptor } from './interceptors/timeout.interceptor';
import { TranslateModule } from '@ngx-translate/core';
import { ButtonModule } from './components/button/button.module';
import { DialogModule } from './components/dialog/dialog.module';
import { initializeKeycloak } from './init/keycloak-init.factory';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AppAuthguard } from './authGuard/app.authguard';
import { ToastComponent } from './components/toast/toast.component';
import {FooterComponent} from "./components/footer/footer.component";

@NgModule({
  declarations: [AppComponent, ToolbarComponent, ToastComponent,FooterComponent],
  imports: [
    AppRoutingModule,
    KeycloakAngularModule,
    BrowserModule,
    ButtonModule,
    DialogModule,
    MessengerInstanceModule,
    HttpClientModule,
    AppRoutingModule,
    NoopAnimationsModule,
    TranslateModule.forRoot(),

  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TimeoutInterceptor,
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService],
    },
    AppAuthguard,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
