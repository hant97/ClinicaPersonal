import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GeneralHistory } from '../models/general-history.model';

@Injectable({
  providedIn: 'root'
})
export class GeneralHistoryService {
  private apiUrl = `${environment.apiUrl}/v1/patients`;

  constructor(private http: HttpClient) { }

  get(patientId: number): Observable<GeneralHistory> {
    return this.http.get<GeneralHistory>(`${this.apiUrl}/${patientId}/general-history`);
  }

  upsert(patientId: number, data: GeneralHistory): Observable<GeneralHistory> {
    return this.http.put<GeneralHistory>(`${this.apiUrl}/${patientId}/general-history`, data);
  }
}
