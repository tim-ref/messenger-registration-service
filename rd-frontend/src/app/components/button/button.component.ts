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

import { Component, Input } from '@angular/core';

@Component({
  selector: 'generic-button',
  templateUrl: './button.component.html',
  styleUrls: ['./button.component.scss'],
})
export class ButtonComponent {
  // Sets the background color
  // Can be any color name defined in tailwind.config
  // Default is 'primary'
  @Input()
  color: string = 'primary';

  // Sets the buttons variant type
  // Applicable values: 'flat', 'outline', 'icon'
  // Default is 'flat'
  @Input()
  variant: string = 'flat';

  // Sets the icon
  // Can be any valid fontawesome icon name or undefined
  // No icon will be displayed if left undefined
  @Input()
  icon: string = '';

  // Sets the position of the icon
  // Applicable values: 'left' or 'right'
  // Default is 'left'
  @Input()
  iconPosition: string = 'left';

  // Sets the color of the icon
  // Can be any color name defined in tailwind.config
  // If no color is provided the icon will use the contrast color
  @Input()
  iconColor: string = '';

  // Determines if the button should be disabled
  // Default is false
  @Input()
  disabled: boolean = false;

  // Determines if the button should have a shadow
  // Default is true
  @Input()
  shadow: boolean = true;
}
