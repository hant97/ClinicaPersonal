import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { PrescriptionService } from './prescription.service';

describe('PrescriptionService', () => {
  let service: PrescriptionService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PrescriptionService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('solicita la página base cero del paciente con el tamaño indicado', () => {
    service.getByPatientId(42, 0, 10).subscribe();

    const request = httpTesting.expectOne(request =>
      request.url.endsWith('/v1/patients/42/prescriptions')
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [], page: { number: 0, size: 10, totalElements: 0, totalPages: 0 } });
  });
});
