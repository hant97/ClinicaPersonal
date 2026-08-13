import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Treatment } from '../models/treatment.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class TreatmentService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Treatment>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Treatment>>(`${this.apiUrl}/patients/${patientId}/treatments`, { params });
  }

  create(patientId: number, data: Treatment): Observable<Treatment> {
    return this.http.post<Treatment>(`${this.apiUrl}/patients/${patientId}/treatments`, data);
  }

  update(id: number, data: Treatment): Observable<Treatment> {
    return this.http.put<Treatment>(`${this.apiUrl}/treatments/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/treatments/${id}`);
  }
}
