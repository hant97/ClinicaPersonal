import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ClinicalSessionDraftService } from './clinical-session-draft.service';

describe('ClinicalSessionDraftService', () => {
  let service: ClinicalSessionDraftService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ClinicalSessionDraftService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía la versión conocida y recibe la nueva al guardar', () => {
    let savedVersion: number | undefined;
    service.saveDraft(7, { subjective: 'nota' }, 3).subscribe(draft => savedVersion = draft.version);

    const request = http.expectOne(request => request.url.endsWith('/v1/clinical-drafts/patients/7'));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ content: { subjective: 'nota' }, version: 3 });
    request.flush({ content: { subjective: 'nota' }, expiresAt: '2026-10-07T12:00:00', version: 4 });
    expect(savedVersion).toBe(4);
  });

  it('condiciona la eliminación a la versión observada', () => {
    service.deleteDraft(7, 4).subscribe();

    const request = http.expectOne(request => request.url.endsWith('/v1/clinical-drafts/patients/7'));
    expect(request.request.method).toBe('DELETE');
    expect(request.request.params.get('version')).toBe('4');
    request.flush(null);
  });
});
