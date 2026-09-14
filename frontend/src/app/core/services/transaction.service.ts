import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Transaction, TransactionInput } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/transactions`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(this.baseUrl);
  }

  get(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`);
  }

  create(transaction: TransactionInput): Observable<Transaction> {
    return this.http.post<Transaction>(this.baseUrl, transaction);
  }
}
