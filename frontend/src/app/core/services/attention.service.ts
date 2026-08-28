import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Attention, AttentionSummary, AttentionStatus, ProfessionalProductivity, UpdateAttentionStatusRequest } from '../models/attention.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AttentionService {
  private apiUrl = `${environment.apiUrl}/v1/attentions`;

  constructor(private http: HttpClient) {}

  getAll(
    searchTerm?: string,
    status?: string,
    professionalId?: number,
    patientId?: number,
    startDate?: string,
    endDate?: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<Attention>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (searchTerm && searchTerm.trim()) {
      params = params.set('searchTerm', searchTerm.trim());
    }
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (professionalId) {
      params = params.set('professionalId', professionalId.toString());
    }
    if (patientId) {
      params = params.set('patientId', patientId.toString());
    }
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<PageResponse<Attention>>(this.apiUrl, { params });
  }

  getTodaySummary(): Observable<AttentionSummary> {
    return this.http.get<AttentionSummary>(`${this.apiUrl}/today-summary`);
  }

  getProductivityReport(dateFrom?: string, dateTo?: string): Observable<ProfessionalProductivity[]> {
    let params = new HttpParams();
    if (dateFrom) {
      params = params.set('dateFrom', dateFrom);
    }
    if (dateTo) {
      params = params.set('dateTo', dateTo);
    }
    return this.http.get<ProfessionalProductivity[]>(`${this.apiUrl}/reports/productivity`, { params });
  }

  getById(id: number): Observable<Attention> {
    return this.http.get<Attention>(`${this.apiUrl}/${id}`);
  }

  create(attention: Partial<Attention>): Observable<Attention> {
    return this.http.post<Attention>(this.apiUrl, attention);
  }

  createFromAppointment(appointmentId: number): Observable<Attention> {
    return this.http.post<Attention>(`${this.apiUrl}/from-appointment/${appointmentId}`, {});
  }

  update(id: number, attention: Partial<Attention>): Observable<Attention> {
    return this.http.put<Attention>(`${this.apiUrl}/${id}`, attention);
  }

  updateStatus(id: number, status: AttentionStatus, notes?: string): Observable<Attention> {
    const payload: UpdateAttentionStatusRequest = { status, notes };
    return this.http.put<Attention>(`${this.apiUrl}/${id}/status`, payload);
  }

  linkSession(attentionId: number, sessionId: number): Observable<Attention> {
    return this.http.post<Attention>(`${this.apiUrl}/${attentionId}/link-session/${sessionId}`, {});
  }

  linkPrescription(attentionId: number, prescriptionId: number): Observable<Attention> {
    return this.http.post<Attention>(`${this.apiUrl}/${attentionId}/link-prescription/${prescriptionId}`, {});
  }

  linkPayment(attentionId: number, paymentId: number): Observable<Attention> {
    return this.http.post<Attention>(`${this.apiUrl}/${attentionId}/link-payment/${paymentId}`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
