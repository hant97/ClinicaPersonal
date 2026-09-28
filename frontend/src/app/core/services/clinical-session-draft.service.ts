import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { getLegacyClinicalDrafts, removeLegacyClinicalDraft } from '../utils/clinical-draft.util';

export interface ClinicalSessionDraft {
  content: Record<string, unknown>;
  expiresAt: string;
}

@Injectable({ providedIn: 'root' })
export class ClinicalSessionDraftService {
  private readonly apiUrl = `${environment.apiUrl}/v1/clinical-drafts/patients`;

  constructor(private readonly http: HttpClient) {}

  getDraft(patientId: number): Observable<ClinicalSessionDraft | null> {
    return this.http.get<ClinicalSessionDraft | null>(`${this.apiUrl}/${patientId}`);
  }

  saveDraft(patientId: number, content: Record<string, unknown>): Observable<ClinicalSessionDraft> {
    return this.http.put<ClinicalSessionDraft>(`${this.apiUrl}/${patientId}`, { content });
  }

  deleteDraft(patientId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${patientId}`);
  }

  migrateLegacyDrafts(username: string | null): Observable<void> {
    if (!username) return of(void 0);

    const migrations = getLegacyClinicalDrafts(username).map(draft =>
      this.getDraft(draft.patientId).pipe(
        switchMap(existingDraft => existingDraft
          ? of(existingDraft)
          : this.saveDraft(draft.patientId, draft.content)),
        tap(() => removeLegacyClinicalDraft(draft.storageKey)),
        map(() => null),
        catchError((error: HttpErrorResponse) => {
          if (error.status === 403 || error.status === 404) {
            removeLegacyClinicalDraft(draft.storageKey);
          }
          return of(null);
        })
      )
    );

    return migrations.length > 0 ? forkJoin(migrations).pipe(map(() => void 0)) : of(void 0);
  }
}
