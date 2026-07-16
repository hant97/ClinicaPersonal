import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RiskAlert } from '../models/risk-alert.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RiskAlertService {
  private apiUrl = `${environment.apiUrl}/patients`;

  constructor(private http: HttpClient) { }

  getAlertsByPatientId(patientId: number, onlyActive: boolean = false): Observable<RiskAlert[]> {
    let params = new HttpParams();
    if (onlyActive) {
      params = params.set('onlyActive', 'true');
    }
    return this.http.get<RiskAlert[]>(`${this.apiUrl}/${patientId}/alerts`, { params });
  }

  createAlert(patientId: number, alert: RiskAlert): Observable<RiskAlert> {
    return this.http.post<RiskAlert>(`${this.apiUrl}/${patientId}/alerts`, alert);
  }

  resolveAlert(patientId: number, alertId: number): Observable<RiskAlert> {
    return this.http.put<RiskAlert>(`${this.apiUrl}/${patientId}/alerts/${alertId}/resolve`, {});
  }
}
