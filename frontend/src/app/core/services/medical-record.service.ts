import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MedicalRecord } from '../models/medical-record.model';

@Injectable({
  providedIn: 'root'
})
export class MedicalRecordService {
  private apiUrl = `${environment.apiUrl}/medical-records`;

  constructor(private http: HttpClient) { }

  getRecordsByPatientId(patientId: number): Observable<MedicalRecord[]> {
    return this.http.get<MedicalRecord[]>(`${this.apiUrl}/patient/${patientId}`);
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
