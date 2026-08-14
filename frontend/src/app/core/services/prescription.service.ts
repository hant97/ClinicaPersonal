import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Prescription } from '../models/prescription.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class PrescriptionService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Prescription>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Prescription>>(`${this.apiUrl}/patients/${patientId}/prescriptions`, { params });
  }

  create(patientId: number, data: Prescription): Observable<Prescription> {
    return this.http.post<Prescription>(`${this.apiUrl}/patients/${patientId}/prescriptions`, data);
  }

  update(id: number, data: Prescription): Observable<Prescription> {
    return this.http.put<Prescription>(`${this.apiUrl}/prescriptions/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/prescriptions/${id}`);
  }
}
