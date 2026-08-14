import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicLanding } from '../models/public-landing.model';
import { WebsiteEditor, WebsiteDraft } from '../models/website-editor.model';

@Injectable({ providedIn: 'root' })
export class WebsiteSettingsService {
  private readonly publicUrl = `${environment.apiUrl}/v1/public/landing`;
  private readonly adminUrl = `${environment.apiUrl}/v1/admin/website`;

  constructor(private readonly http: HttpClient) {}

  getPublicLanding(): Observable<PublicLanding> { return this.http.get<PublicLanding>(this.publicUrl); }

  getEditor(): Observable<WebsiteEditor> { return this.http.get<WebsiteEditor>(`${this.adminUrl}/editor`); }
  saveDraft(draft: WebsiteDraft, revision: number): Observable<WebsiteEditor> { return this.http.put<WebsiteEditor>(`${this.adminUrl}/draft`, draft, { params: { revision } }); }
  publish(revision: number): Observable<WebsiteEditor> { return this.http.post<WebsiteEditor>(`${this.adminUrl}/publish`, null, { params: { revision } }); }
  resetDraft(revision: number): Observable<WebsiteEditor> { return this.http.post<WebsiteEditor>(`${this.adminUrl}/draft/reset`, null, { params: { revision } }); }
  uploadDraftAsset(category: string, file: File): Observable<WebsiteEditor> { const data = new FormData(); data.append('file', file); return this.http.post<WebsiteEditor>(`${this.adminUrl}/draft/assets/${category}`, data); }
  deleteDraftAsset(category: string): Observable<WebsiteEditor> { return this.http.delete<WebsiteEditor>(`${this.adminUrl}/draft/assets/${category}`); }
  uploadDraftProfessionalPhoto(draftKey: string, file: File): Observable<WebsiteEditor> { const data = new FormData(); data.append('file', file); return this.http.post<WebsiteEditor>(`${this.adminUrl}/draft/professionals/${draftKey}/photo`, data); }
}
