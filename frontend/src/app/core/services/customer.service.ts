import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Customer, CustomerInput } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/customers`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.baseUrl);
  }

  create(customer: CustomerInput): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, customer);
  }

  update(id: string, customer: CustomerInput): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, customer);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
