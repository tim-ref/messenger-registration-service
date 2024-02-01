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
import { AppService } from '../app.service';

let appService: AppService;

describe('AppService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [AppService] });
    appService = TestBed.inject(AppService);
  });

  it('should call open toast observable', (done: DoneFn) => {
    appService.toast$.subscribe((value) => {
      expect(value).toEqual({
        description: 'Error',
        icon: 'fa-triangle-exclamation',
        iconColor: 'primary',
        timeout: 1000,
      });
      done();
    });

    appService.showToast({
      description: 'Error',
      icon: 'fa-triangle-exclamation',
      iconColor: 'primary',
      timeout: 1000,
    });
  });
});
