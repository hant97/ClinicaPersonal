import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClinicalHistory } from '../models/clinical-history.model';

@Injectable({
  providedIn: 'root'
})
export class ClinicalHistoryService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  get(patientId: number): Observable<ClinicalHistory> {
    return this.http.get<ClinicalHistory>(`${this.apiUrl}/patients/${patientId}/clinical-history`);
  }
}
