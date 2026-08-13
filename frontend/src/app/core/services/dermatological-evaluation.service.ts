import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DermatologicalEvaluation } from '../models/dermatological-evaluation.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class DermatologicalEvaluationService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) {}

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<DermatologicalEvaluation>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<DermatologicalEvaluation>>(
      `${this.apiUrl}/patients/${patientId}/dermatological-evaluations`,
      { params }
    );
  }

  getById(id: number): Observable<DermatologicalEvaluation> {
    return this.http.get<DermatologicalEvaluation>(`${this.apiUrl}/dermatological-evaluations/${id}`);
  }

  create(patientId: number, data: DermatologicalEvaluation): Observable<DermatologicalEvaluation> {
    return this.http.post<DermatologicalEvaluation>(`${this.apiUrl}/patients/${patientId}/dermatological-evaluations`, data);
  }

  update(id: number, data: DermatologicalEvaluation): Observable<DermatologicalEvaluation> {
    return this.http.put<DermatologicalEvaluation>(`${this.apiUrl}/dermatological-evaluations/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/dermatological-evaluations/${id}`);
  }
}
