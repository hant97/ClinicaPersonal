import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Lesion } from '../models/lesion.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class LesionService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Lesion>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Lesion>>(`${this.apiUrl}/patients/${patientId}/lesions`, { params });
  }

  create(patientId: number, data: Lesion): Observable<Lesion> {
    return this.http.post<Lesion>(`${this.apiUrl}/patients/${patientId}/lesions`, data);
  }

  update(id: number, data: Lesion): Observable<Lesion> {
    return this.http.put<Lesion>(`${this.apiUrl}/lesions/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/lesions/${id}`);
  }
}
