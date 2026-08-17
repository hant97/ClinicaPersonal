import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment, PaymentSummary } from '../models/payment.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

export interface PaymentFilters {
  searchTerm?: string;
  dateFrom?: string;
  dateTo?: string;
  paymentMethod?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = `${environment.apiUrl}/v1/payments`;

  constructor(private http: HttpClient) { }

  getAll(page: number = 0, size: number = 10, filters?: PaymentFilters): Observable<PageResponse<Payment>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (filters?.searchTerm) {
      params = params.set('searchTerm', filters.searchTerm);
    }
    if (filters?.dateFrom) {
      params = params.set('dateFrom', filters.dateFrom);
    }
    if (filters?.dateTo) {
      params = params.set('dateTo', filters.dateTo);
    }
    if (filters?.paymentMethod) {
      params = params.set('paymentMethod', filters.paymentMethod);
    }
    return this.http.get<PageResponse<Payment>>(this.apiUrl, { params });
  }

  getSummary(): Observable<PaymentSummary> {
    return this.http.get<PaymentSummary>(`${this.apiUrl}/summary`);
  }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Payment>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Payment>>(`${this.apiUrl}/patient/${patientId}`, { params });
  }

  create(payment: Payment): Observable<Payment> {
    return this.http.post<Payment>(this.apiUrl, payment);
  }

  update(id: number, payment: Payment): Observable<Payment> {
    return this.http.put<Payment>(`${this.apiUrl}/${id}`, payment);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
