/*
 * Copyright (C) 2025 akquinet GmbH
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

import {Component, Input} from '@angular/core';
import {AbstractControl, FormControl} from "@angular/forms";
import {SelectOption} from "./model/select-option";

@Component({
  selector: 'generic-select',
  templateUrl: './select.component.html',
  styleUrl: './select.component.scss'
})
export class SelectComponent {
  @Input()
  name: string = '';

  @Input()
  control: AbstractControl | null = null;

  @Input()
  options: SelectOption[] = [];

  @Input()
  placeholder: string | null = null;

  get formControl(): FormControl {
    return this.control as FormControl;
  }
}
