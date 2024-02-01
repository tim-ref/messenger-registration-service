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
import { Observable } from 'rxjs';
import {
  HttpClient,
  HttpEvent,
  HttpHeaders,
  HttpResponse,
} from '@angular/common/http';
import { environment } from '../../environments/environment';
import {Version} from "../models/version";

@Injectable({
  providedIn: 'root',
})
export class RestService {
  readonly contentType = 'application/json';
  constructor(private http: HttpClient) {}

  public postToApi<T>(
    data: any,
    route: string,
    options: any = {}
  ): Observable<T> {
    return this.http.post<T>(
      environment.apiUrl + route,
      data,
      options
    ) as Observable<T>;
  }

  public putToApi<T>(
    data: any,
    route: string,
    options: any = {}
  ): Observable<T | HttpEvent<T>> {
    let headers = new HttpHeaders({ 'Content-Type': this.contentType });

    return this.http.put<T>(environment.apiUrl + route, data, {
      ...options,
      headers: headers,
    });
  }

  public getFromApi<T>(route: string): Observable<T> {
    return this.http.get<T>(environment.apiUrl + route);
  }
  public getFileFromApi<T>(route: string): Observable<any> {
    return this.http.get(environment.apiUrl + route,
      {responseType: 'blob'});
  }

  public deleteFromApi(route: string): Observable<any> {
    return this.http.delete(environment.apiUrl + route);
  }

  public getVersion(route: string): Observable<Version[]> {
    return this.http.get<Version[]>(environment.fachdienstMetaUrl + route);
  }
}
