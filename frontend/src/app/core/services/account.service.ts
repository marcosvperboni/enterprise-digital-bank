import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Account, AccountInput } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/accounts`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Account[]> {
    return this.http.get<Account[]>(this.baseUrl);
  }

  create(account: AccountInput): Observable<Account> {
    return this.http.post<Account>(this.baseUrl, account);
  }

  update(id: string, account: AccountInput): Observable<Account> {
    return this.http.put<Account>(`${this.baseUrl}/${id}`, account);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
