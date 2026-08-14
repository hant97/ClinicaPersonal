import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ClinicalDocumentService } from './clinical-document.service';

describe('ClinicalDocumentService', () => {
  let service: ClinicalDocumentService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ClinicalDocumentService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('solicita la página base cero del paciente con el tamaño indicado', () => {
    service.getByPatientId(42, 0, 10).subscribe();

    const request = httpTesting.expectOne(request =>
      request.url.endsWith('/v1/patients/42/clinical-documents')
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [], page: { number: 0, size: 10, totalElements: 0, totalPages: 0 } });
  });

  it('sube el documento como multipart con categoría y fecha', () => {
    const file = new File(['contenido'], 'consentimiento.pdf', { type: 'application/pdf' });
    service.upload(42, file, 'CONSENTIMIENTO', undefined, '2026-08-14').subscribe();

    const request = httpTesting.expectOne(request =>
      request.url.endsWith('/v1/patients/42/clinical-documents')
    );
    expect(request.request.method).toBe('POST');
    const body = request.request.body as FormData;
    expect(body.get('category')).toBe('CONSENTIMIENTO');
    expect(body.get('documentDate')).toBe('2026-08-14');
    request.flush({ id: 1, patientId: 42, name: 'consentimiento', category: 'CONSENTIMIENTO' });
  });
});
