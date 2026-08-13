import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Diagnosis } from '../models/diagnosis.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class DiagnosisService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Diagnosis>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Diagnosis>>(`${this.apiUrl}/patients/${patientId}/diagnoses`, { params });
  }

  create(patientId: number, data: Diagnosis): Observable<Diagnosis> {
    return this.http.post<Diagnosis>(`${this.apiUrl}/patients/${patientId}/diagnoses`, data);
  }

  update(id: number, data: Diagnosis): Observable<Diagnosis> {
    return this.http.put<Diagnosis>(`${this.apiUrl}/diagnoses/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/diagnoses/${id}`);
  }
}
