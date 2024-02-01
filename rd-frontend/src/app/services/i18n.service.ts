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

import { Injectable } from '@angular/core';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

const languageKey = 'language';

@Injectable({
  providedIn: 'root',
})
export class I18nService {
  defaultLanguage!: string;
  supportedLanguages!: string[];

  private langChangeSubscription!: Subscription;

  constructor(private translateService: TranslateService) {
    this.init('de', ['de']);
  }

  /**
   * Initializes i18n for the application.
   * Loads language from local storage if present, or sets default language.
   * @param defaultLanguage The default language to use.
   * @param supportedLanguages The list of supported languages.
   */
  init(defaultLanguage: string, supportedLanguages: string[]) {
    this.defaultLanguage = defaultLanguage;
    this.supportedLanguages = supportedLanguages;
    this.language = '';

    // Warning: this subscription will always be alive for the app's lifetime
    this.langChangeSubscription = this.translateService.onLangChange.subscribe(
      (event: LangChangeEvent) => {
        localStorage.setItem(languageKey, event.lang);
      }
    );
  }

  /**
   * Add translations for language. This can be called when initializing a new module or component
   * with separate translation files. New translation items are merged with existing ones.
   * @param lang
   * @param translation
   */
  addTranslation(lang: string, translation: Object): void {
    this.translateService.setTranslation(lang, translation, true);
  }

  /**
   * Cleans up language change subscription.
   */
  destroy() {
    if (this.langChangeSubscription) {
      this.langChangeSubscription.unsubscribe();
    }
  }

  /**
   * Sets the current language.
   * Note: The current language is saved to the local storage.
   * If no parameter is specified, the language is loaded from local storage (if present).
   * @param language The IETF language code to set.
   */
  set language(language: string) {
    let newLanguage =
      language ||
      localStorage.getItem(languageKey) ||
      this.translateService.getBrowserCultureLang() ||
      '';
    let isSupportedLanguage = this.supportedLanguages.includes(newLanguage);

    // If no exact match is found, search without the region
    if (newLanguage && !isSupportedLanguage) {
      newLanguage = newLanguage.split('-')[0];
      newLanguage =
        this.supportedLanguages.find((supportedLanguage) =>
          supportedLanguage.startsWith(newLanguage)
        ) || '';
      isSupportedLanguage = Boolean(newLanguage);
    }

    // Fallback if language is not supported
    if (!newLanguage || !isSupportedLanguage) {
      newLanguage = this.defaultLanguage;
    }

    language = newLanguage;

    this.translateService.use(language);
  }

  /**
   * Gets the current language.
   * @return The current language code.
   */
  get language(): string {
    return this.translateService.currentLang;
  }
}
