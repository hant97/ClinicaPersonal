import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DermatologicalHistory } from '../models/dermatological-history.model';

@Injectable({
  providedIn: 'root'
})
export class DermatologicalHistoryService {
  private apiUrl = `${environment.apiUrl}/v1/patients`;

  constructor(private http: HttpClient) { }

  get(patientId: number): Observable<DermatologicalHistory> {
    return this.http.get<DermatologicalHistory>(`${this.apiUrl}/${patientId}/dermatological-history`);
  }

  upsert(patientId: number, data: DermatologicalHistory): Observable<DermatologicalHistory> {
    return this.http.put<DermatologicalHistory>(`${this.apiUrl}/${patientId}/dermatological-history`, data);
  }
}
