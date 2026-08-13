import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LesionPhoto } from '../models/lesion-photo.model';

@Injectable({
  providedIn: 'root'
})
export class LesionPhotoService {
  private apiUrl = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) { }

  getPhotos(lesionId: number): Observable<LesionPhoto[]> {
    return this.http.get<LesionPhoto[]>(`${this.apiUrl}/lesions/${lesionId}/photos`);
  }

  upload(lesionId: number, file: File, description?: string, takenDate?: string): Observable<LesionPhoto> {
    const formData = new FormData();
    formData.append('file', file);
    if (description) {
      formData.append('description', description);
    }
    if (takenDate) {
      formData.append('takenDate', takenDate);
    }
    return this.http.post<LesionPhoto>(`${this.apiUrl}/lesions/${lesionId}/photos`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/lesion-photos/${id}`);
  }

  getPhotoFile(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/lesion-photos/${id}/file`, { responseType: 'blob' });
  }
}
