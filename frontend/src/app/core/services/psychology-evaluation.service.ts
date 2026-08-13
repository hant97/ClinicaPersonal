import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PsychologyEvaluation } from '../models/psychology-evaluation.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class PsychologyEvaluationService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<PsychologyEvaluation>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<PsychologyEvaluation>>(`${this.apiUrl}/patients/${patientId}/psychology-evaluations`, { params });
  }

  getById(id: number): Observable<PsychologyEvaluation> {
    return this.http.get<PsychologyEvaluation>(`${this.apiUrl}/psychology-evaluations/${id}`);
  }

  create(patientId: number, data: PsychologyEvaluation): Observable<PsychologyEvaluation> {
    return this.http.post<PsychologyEvaluation>(`${this.apiUrl}/patients/${patientId}/psychology-evaluations`, data);
  }

  update(id: number, data: PsychologyEvaluation): Observable<PsychologyEvaluation> {
    return this.http.put<PsychologyEvaluation>(`${this.apiUrl}/psychology-evaluations/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/psychology-evaluations/${id}`);
  }
}
