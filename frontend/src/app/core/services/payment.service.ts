import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment } from '../models/payment.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = `${environment.apiUrl}/v1/payments`;

  constructor(private http: HttpClient) { }

  getAll(page: number = 0, size: number = 10): Observable<PageResponse<Payment>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Payment>>(this.apiUrl, { params });
  }

  getByPatientId(patientId: number): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.apiUrl}/patient/${patientId}`);
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
