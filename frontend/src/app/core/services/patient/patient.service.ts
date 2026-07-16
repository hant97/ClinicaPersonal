import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Patient } from '../../models/patient.model';
import { PageResponse } from '../../models/page.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PatientService {
  private apiUrl = `${environment.apiUrl}/v1/patients`;

  constructor(private http: HttpClient) { }

  getAll(page: number = 0, size: number = 10): Observable<PageResponse<Patient>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Patient>>(this.apiUrl, { params });
  }

  search(query: string, page: number = 0, size: number = 10): Observable<PageResponse<Patient>> {
    let params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Patient>>(`${this.apiUrl}/search`, { params });
  }

  getById(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.apiUrl}/${id}`);
  }

  create(patient: Patient): Observable<Patient> {
    return this.http.post<Patient>(this.apiUrl, patient);
  }

  update(id: number, patient: Patient): Observable<Patient> {
    return this.http.put<Patient>(`${this.apiUrl}/${id}`, patient);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
