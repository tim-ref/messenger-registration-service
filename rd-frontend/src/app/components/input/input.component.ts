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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AbstractControl, FormControl } from '@angular/forms';

@Component({
  selector: 'generic-input',
  templateUrl: './input.component.html',
  styleUrls: ['./input.component.scss'],
})
export class InputComponent {
  // The name of the input field, also displayed as a placeholder
  @Input()
  name: string = '';

  // Sets the icon displayed before or behind the input field
  @Input()
  icon: string | undefined;

  // Sets the icon position within the input field
  // Applicable values: 'left', 'right'
  @Input()
  iconPosition: string = 'left';

  // The form control for the input field
  @Input()
  control: AbstractControl | null = null;

  @Input()
  type: string = 'input';

  @Output()
  iconClick = new EventEmitter();

  getControl(): FormControl {
    return this.control as FormControl;
  }
}
