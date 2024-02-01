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

import {Component, OnInit} from '@angular/core';
import {RestService} from "../../services/rest.service";
import {Version} from "../../models/version";


enum ModuleName {
  FD = 'Fachdienst',
  RD = 'Registration Dienst',
  MP = 'Messenger Proxy',
}

@Component({
  selector: 'admin-footer',
  templateUrl: './footer.component.html'
})
export class FooterComponent implements OnInit {
  public versionList: Version[] = [];
  private readonly versionApiUrl = '/meta/version';

  constructor(
    private restService: RestService,
  ) {
  }

  ngOnInit(): void {
    this.loadVersionList()
  }

  loadVersionList(): void {
    this.restService.getVersion(this.versionApiUrl).subscribe((data: Version[]) => {
      this.versionList = data.filter(version => version.component in ModuleName);
    });
  }

  getModuleName(abbreviation: string): string {
    return ModuleName[abbreviation as keyof typeof ModuleName] || '';
  }
}
