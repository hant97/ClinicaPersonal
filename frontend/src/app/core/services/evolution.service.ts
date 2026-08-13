import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Evolution } from '../models/evolution.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class EvolutionService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Evolution>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Evolution>>(`${this.apiUrl}/patients/${patientId}/evolutions`, { params });
  }

  create(patientId: number, data: Evolution): Observable<Evolution> {
    return this.http.post<Evolution>(`${this.apiUrl}/patients/${patientId}/evolutions`, data);
  }

  update(id: number, data: Evolution): Observable<Evolution> {
    return this.http.put<Evolution>(`${this.apiUrl}/evolutions/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/evolutions/${id}`);
  }
}
