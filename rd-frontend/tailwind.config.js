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

module.exports = {
  content: ["./src/**/*.{html, ts, scss}"],
  theme: {
    colors: {
      primary: "#de1f2e",
      "primary-contrast": "#ffffff",
      accent: "#424243",
      "accent-contrast": "#ffffff",
      white: "#ffffff",
      "white-contrast": "#424243",
      black: "#121212",
      "black-contrast": "#ffffff",
      lightBlue: "#92abe1",
      "lightBlue-contrast": "#ffffff",
      blue: "#274775",
      "blue-contrast": "#ffffff",
      green: "#78b700",
      "green-contrast": "#ffffff",
    },
  },
  safelist: [
    {
      pattern: /(from|via|to|border|bg|text)-(.*)/,
    },
  ],
  plugins: [],
};
