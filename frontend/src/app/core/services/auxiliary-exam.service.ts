import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuxiliaryExam } from '../models/auxiliary-exam.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class AuxiliaryExamService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<AuxiliaryExam>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<AuxiliaryExam>>(`${this.apiUrl}/patients/${patientId}/auxiliary-exams`, { params });
  }

  create(patientId: number, data: AuxiliaryExam): Observable<AuxiliaryExam> {
    return this.http.post<AuxiliaryExam>(`${this.apiUrl}/patients/${patientId}/auxiliary-exams`, data);
  }

  update(id: number, data: AuxiliaryExam): Observable<AuxiliaryExam> {
    return this.http.put<AuxiliaryExam>(`${this.apiUrl}/auxiliary-exams/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/auxiliary-exams/${id}`);
  }
}
