import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TherapeuticPlan } from '../models/therapeutic-plan.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class TherapeuticPlanService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<TherapeuticPlan>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<TherapeuticPlan>>(`${this.apiUrl}/patients/${patientId}/therapeutic-plans`, { params });
  }

  create(patientId: number, data: TherapeuticPlan): Observable<TherapeuticPlan> {
    return this.http.post<TherapeuticPlan>(`${this.apiUrl}/patients/${patientId}/therapeutic-plans`, data);
  }

  update(id: number, data: TherapeuticPlan): Observable<TherapeuticPlan> {
    return this.http.put<TherapeuticPlan>(`${this.apiUrl}/therapeutic-plans/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/therapeutic-plans/${id}`);
  }
}
