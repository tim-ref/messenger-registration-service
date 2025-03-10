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

import {CommonModule} from "@angular/common";
import {Component} from "@angular/core";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormControl, ReactiveFormsModule} from "@angular/forms";
import {enumValuesToSelectOptions} from "./model/select-option";

import {SelectComponent} from './select.component';
import {SelectModule} from "./select.module";

enum Color {
  Red = 'RED',
  Green = 'GREEN',
  Blue = 'BLUE'
}

const selectOptions = enumValuesToSelectOptions(Color);

describe('SelectComponent', () => {
  let component: SelectComponent;
  let fixture: ComponentFixture<SelectComponent>;
  let selectElement: HTMLSelectElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SelectComponent],
      imports: [CommonModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SelectComponent);
    component = fixture.componentInstance;
    selectElement = fixture.nativeElement.querySelector('select');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show Enum options', ()=> {
    component.options = selectOptions;
    fixture.detectChanges();

    const options = selectElement.querySelectorAll('option');
    expect(options).toHaveSize(3);
    expect(options[0].textContent).toContain('RED');
    expect(options[1].textContent).toContain('GREEN');
    expect(options[2].textContent).toContain('BLUE');
  });

  it('should show and set correct value', () => {
    component.options = selectOptions;
    fixture.detectChanges();

    selectElement.value = 'GREEN';
    fixture.detectChanges();

    expect(selectElement.value).toBe('GREEN');
  });
});

@Component({
  template: `<generic-select name="test" [options]="selectOptions" [control]="colorControl"></generic-select>`
})
class TestHostComponent {
  colorControl = new FormControl();
  protected readonly selectOptions = selectOptions;
}

describe('SelectComponent integrated in FormControl', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;
  let selectElement: HTMLSelectElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, SelectModule],
      declarations: [TestHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    selectElement = fixture.nativeElement.querySelector('select');
  });

  it('should set and show correct value', () => {
    hostComponent.colorControl.setValue('RED');
    fixture.detectChanges();

    expect(selectElement.value).toBe('0: RED');
  });

  it('should forward changes to FormControl', () => {
    hostComponent.colorControl.setValue('1: GREEN');
    fixture.detectChanges();

    selectElement.value = '2: BLUE';
    selectElement.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(hostComponent.colorControl.value).toBe('BLUE');
  });
});



