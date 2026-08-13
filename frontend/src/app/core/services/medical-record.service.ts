import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MedicalRecord } from '../models/medical-record.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class MedicalRecordService {
  private apiUrl = `${environment.apiUrl}/v1/medical-records`;

  constructor(private http: HttpClient) { }

  getRecordsByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<MedicalRecord>> {
    return this.http.get<PageResponse<MedicalRecord>>(`${this.apiUrl}/patient/${patientId}`, {
      params: { page, size }
    });
  }

  createRecord(record: MedicalRecord): Observable<MedicalRecord> {
    return this.http.post<MedicalRecord>(this.apiUrl, record);
  }

  updateRecord(id: number, record: MedicalRecord): Observable<MedicalRecord> {
    return this.http.put<MedicalRecord>(`${this.apiUrl}/${id}`, record);
  }

  deleteRecord(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
