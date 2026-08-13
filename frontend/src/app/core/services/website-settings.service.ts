import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicLanding } from '../models/public-landing.model';
import { WebsiteSettingsAdmin } from '../models/website-settings.model';

@Injectable({ providedIn: 'root' })
export class WebsiteSettingsService {
  private readonly publicUrl = `${environment.apiUrl}/v1/public/landing`;
  private readonly adminUrl = `${environment.apiUrl}/v1/admin/website`;

  constructor(private readonly http: HttpClient) {}

  getPublicLanding(): Observable<PublicLanding> { return this.http.get<PublicLanding>(this.publicUrl); }
  getAdminSettings(): Observable<WebsiteSettingsAdmin> { return this.http.get<WebsiteSettingsAdmin>(this.adminUrl); }
  update(settings: WebsiteSettingsAdmin): Observable<WebsiteSettingsAdmin> { return this.http.put<WebsiteSettingsAdmin>(this.adminUrl, settings); }
  uploadAsset(category: string, file: File): Observable<WebsiteSettingsAdmin> { const data = new FormData(); data.append('file', file); return this.http.post<WebsiteSettingsAdmin>(`${this.adminUrl}/assets/${category}`, data); }
  deleteAsset(category: string): Observable<WebsiteSettingsAdmin> { return this.http.delete<WebsiteSettingsAdmin>(`${this.adminUrl}/assets/${category}`); }
  uploadProfessionalPhoto(id: number, file: File): Observable<WebsiteSettingsAdmin> { const data = new FormData(); data.append('file', file); return this.http.post<WebsiteSettingsAdmin>(`${this.adminUrl}/professionals/${id}/photo`, data); }
}
