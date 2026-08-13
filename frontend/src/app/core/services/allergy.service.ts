import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Allergy } from '../models/allergy.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class AllergyService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Allergy>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Allergy>>(`${this.apiUrl}/patients/${patientId}/allergies`, { params });
  }

  create(patientId: number, data: Allergy): Observable<Allergy> {
    return this.http.post<Allergy>(`${this.apiUrl}/patients/${patientId}/allergies`, data);
  }

  update(id: number, data: Allergy): Observable<Allergy> {
    return this.http.put<Allergy>(`${this.apiUrl}/allergies/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/allergies/${id}`);
  }
}
