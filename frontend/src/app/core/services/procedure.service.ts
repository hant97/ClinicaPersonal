import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Procedure } from '../models/procedure.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class ProcedureService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Procedure>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Procedure>>(`${this.apiUrl}/patients/${patientId}/procedures`, { params });
  }

  create(patientId: number, data: Procedure): Observable<Procedure> {
    return this.http.post<Procedure>(`${this.apiUrl}/patients/${patientId}/procedures`, data);
  }

  update(id: number, data: Procedure): Observable<Procedure> {
    return this.http.put<Procedure>(`${this.apiUrl}/procedures/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/procedures/${id}`);
  }
}
