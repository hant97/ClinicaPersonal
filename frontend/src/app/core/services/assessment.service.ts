import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Assessment, PsychometricTest } from '../models/assessment.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AssessmentService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getAvailableTests(): Observable<PsychometricTest[]> {
    return this.http.get<PsychometricTest[]>(`${this.apiUrl}/tests`);
  }

  getAssessmentsByPatient(patientId: number): Observable<Assessment[]> {
    return this.http.get<Assessment[]>(`${this.apiUrl}/assessments/patient/${patientId}`);
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
