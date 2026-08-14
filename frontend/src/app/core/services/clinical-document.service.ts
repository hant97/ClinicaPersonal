import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClinicalDocument } from '../models/clinical-document.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class ClinicalDocumentService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getByPatientId(patientId: number, page: number = 0, size: number = 10): Observable<PageResponse<ClinicalDocument>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<ClinicalDocument>>(`${this.apiUrl}/patients/${patientId}/clinical-documents`, { params });
  }

  upload(patientId: number, file: File, category?: string, name?: string, documentDate?: string): Observable<ClinicalDocument> {
    const formData = new FormData();
    formData.append('file', file);
    if (category) {
      formData.append('category', category);
    }
    if (name) {
      formData.append('name', name);
    }
    if (documentDate) {
      formData.append('documentDate', documentDate);
    }
    return this.http.post<ClinicalDocument>(`${this.apiUrl}/patients/${patientId}/clinical-documents`, formData);
  }

  getFile(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/clinical-documents/${id}/file`, { responseType: 'blob' });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clinical-documents/${id}`);
  }
}
