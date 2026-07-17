import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { ClinicalService } from '../models/clinical-service.model';

@Injectable({
  providedIn: 'root'
})
export class ClinicalServiceService {

  private apiUrl = `${environment.apiUrl}/clinical-services`;

  constructor(private http: HttpClient) { }

  getAllServices(searchTerm: string = '', page: number = 0, size: number = 10): Observable<PageResponse<ClinicalService>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (searchTerm) {
      params = params.set('name', searchTerm);
    }
    return this.http.get<PageResponse<ClinicalService>>(this.apiUrl, { params });
  }

  getAllActiveServices(): Observable<ClinicalService[]> {
    return this.http.get<ClinicalService[]>(`${this.apiUrl}/active`);
  }

  getServiceById(id: number): Observable<ClinicalService> {
    return this.http.get<ClinicalService>(`${this.apiUrl}/${id}`);
  }

  createService(service: ClinicalService): Observable<ClinicalService> {
    return this.http.post<ClinicalService>(this.apiUrl, service);
  }

  updateService(id: number, service: ClinicalService): Observable<ClinicalService> {
    return this.http.put<ClinicalService>(`${this.apiUrl}/${id}`, service);
  }

  deleteService(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
