import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { ClinicalService, ClinicalServiceStats } from '../models/clinical-service.model';

export interface ClinicalServiceFilters {
  name?: string;
  category?: string;
  active?: boolean | null;
  minPrice?: number | null;
  maxPrice?: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class ClinicalServiceService {

  private apiUrl = `${environment.apiUrl}/v1/clinical-services`;

  constructor(private http: HttpClient) { }

  getAllServices(page: number = 0, size: number = 10, filters?: ClinicalServiceFilters): Observable<PageResponse<ClinicalService>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (filters?.name) {
      params = params.set('name', filters.name);
    }
    if (filters?.category) {
      params = params.set('category', filters.category);
    }
    if (filters?.active !== undefined && filters?.active !== null) {
      params = params.set('active', filters.active.toString());
    }
    if (filters?.minPrice !== undefined && filters?.minPrice !== null) {
      params = params.set('minPrice', filters.minPrice.toString());
    }
    if (filters?.maxPrice !== undefined && filters?.maxPrice !== null) {
      params = params.set('maxPrice', filters.maxPrice.toString());
    }
    return this.http.get<PageResponse<ClinicalService>>(this.apiUrl, { params });
  }

  getStats(): Observable<ClinicalServiceStats> {
    return this.http.get<ClinicalServiceStats>(`${this.apiUrl}/stats`);
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
