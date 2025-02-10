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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AppService} from '../../../../../../services/app.service';
import {DialogService} from '../../../../../../services/dialog.service';
import {ReactiveFormsModule} from '@angular/forms';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {TranslateModule} from '@ngx-translate/core';
import {By} from '@angular/platform-browser';
import {ButtonModule} from '../../../../../../components/button/button.module';
import {DialogModule} from '../../../../../../components/dialog/dialog.module';
import {LoadingIndicatorModule} from '../../../../../../components/loading-indicator/loading-indicator.module';
import {
    DialogServiceStub,
    AppServiceStub,
} from '../../../../../../../test/stubs';
import {
    TimVersionSelectionDialogComponent
} from "./tim-version-selection-dialog.component";


describe('TimVersionSelectionDialogComponent', () => {
    let fixture: ComponentFixture<TimVersionSelectionDialogComponent>;
    let component: TimVersionSelectionDialogComponent;
    let dialogService: DialogService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TimVersionSelectionDialogComponent],
            imports: [

                ButtonModule,
                DialogModule,
                LoadingIndicatorModule,
                ReactiveFormsModule,
                HttpClientTestingModule,
                TranslateModule.forRoot(),
            ],
            providers: [
                {provide: DialogService, useClass: DialogServiceStub},
                {provide: AppService, useClass: AppServiceStub},
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(TimVersionSelectionDialogComponent);
        component = fixture.componentInstance;
        dialogService = TestBed.inject(DialogService);
    });

    it('should create the component', () => {
        fixture.detectChanges();
        expect(component).toBeTruthy();
    });

    it('should display the loading indicator while loading', () => {
        component.loading = true;
        fixture.detectChanges();

        const loadingIndicator = fixture.debugElement.query(By.css('.loading-spinner'));
        expect(loadingIndicator).toBeTruthy();
    });


    it('should trigger requestInstance on button click', () => {
        spyOn(component, 'requestInstance').and.callThrough();
        fixture.detectChanges();
        const button = fixture.debugElement.query(By.css('#request-instance-button'));
        button.triggerEventHandler('click', null);

        expect(component.requestInstance).toHaveBeenCalled();
    });

    it('should close the dialog with selected variant on requestInstance', () => {
        spyOn(dialogService, 'closeDialog');
        fixture.detectChanges();
        component.formGroup.get('timVariantSelector')?.setValue('Option1');
        component.requestInstance();

        expect(dialogService.closeDialog).toHaveBeenCalledWith({
            createInstance: true,
            timVersion: 'Option1',
        });
    });

    it('should close the dialog without creating an instance on closeDialog', () => {
        spyOn(dialogService, 'closeDialog');
        fixture.detectChanges();
        component.closeDialog();

        expect(dialogService.closeDialog).toHaveBeenCalledWith({createInstance: false});
    });
});
