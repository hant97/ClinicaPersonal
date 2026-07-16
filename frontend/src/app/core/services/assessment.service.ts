import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Assessment, PsychometricTest } from '../models/assessment.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AssessmentService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getAvailableTests(page: number = 0, size: number = 10): Observable<PageResponse<PsychometricTest>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<PsychometricTest>>(`${this.apiUrl}/tests`, { params });
  }

  getAssessmentsByPatient(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<Assessment>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Assessment>>(`${this.apiUrl}/assessments/patient/${patientId}`, { params });
  }

  saveAssessment(assessment: Assessment): Observable<Assessment> {
    return this.http.post<Assessment>(`${this.apiUrl}/assessments`, assessment);
  }

  getTestById(id: number): Observable<PsychometricTest> {
    return this.http.get<PsychometricTest>(`${this.apiUrl}/tests/${id}`);
  }

  createTest(test: PsychometricTest): Observable<PsychometricTest> {
    return this.http.post<PsychometricTest>(`${this.apiUrl}/tests`, test);
  }

  updateTest(id: number, test: PsychometricTest): Observable<PsychometricTest> {
    return this.http.put<PsychometricTest>(`${this.apiUrl}/tests/${id}`, test);
  }

  deleteTest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/tests/${id}`);
  }
}
