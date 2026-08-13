import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Medication } from '../models/medication.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class MedicationService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Medication>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Medication>>(`${this.apiUrl}/patients/${patientId}/medications`, { params });
  }

  create(patientId: number, data: Medication): Observable<Medication> {
    return this.http.post<Medication>(`${this.apiUrl}/patients/${patientId}/medications`, data);
  }

  update(id: number, data: Medication): Observable<Medication> {
    return this.http.put<Medication>(`${this.apiUrl}/medications/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/medications/${id}`);
  }
}
