import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClinicalSession } from '../models/clinical-session.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ClinicalSessionService {
  private apiUrl = `${environment.apiUrl}/clinical-sessions`;

  constructor(private http: HttpClient) { }

  getSessionsByPatientId(patientId: number): Observable<ClinicalSession[]> {
    return this.http.get<ClinicalSession[]>(`${this.apiUrl}/patient/${patientId}`);
  }

  getSessionById(id: number): Observable<ClinicalSession> {
    return this.http.get<ClinicalSession>(`${this.apiUrl}/${id}`);
  }

  createSession(session: ClinicalSession): Observable<ClinicalSession> {
    return this.http.post<ClinicalSession>(this.apiUrl, session);
  }

  updateSession(id: number, session: ClinicalSession): Observable<ClinicalSession> {
    return this.http.put<ClinicalSession>(`${this.apiUrl}/${id}`, session);
  }

  deleteSession(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
