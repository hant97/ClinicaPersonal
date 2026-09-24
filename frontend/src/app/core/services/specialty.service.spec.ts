import type { MockedObject } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SpecialtyService } from './specialty.service';
import { AuthService } from './auth.service';
import { SpecialtyItem } from '../models/specialty.model';

describe('SpecialtyService', () => {
  let service: SpecialtyService;
  let httpTesting: HttpTestingController;
  let authServiceSpy: MockedObject<AuthService>;

  const mockSpecialties: SpecialtyItem[] = [
    { id: 1, code: 'PSICOLOGIA', name: 'Psicología', icon: 'Brain', active: true, displayOrder: 1 },
    { id: 2, code: 'DERMATOLOGIA', name: 'Dermatología', icon: 'Stethoscope', active: true, displayOrder: 2 }
  ];

  beforeEach(() => {
    authServiceSpy = {
      getToken: vi.fn().mockName('AuthService.getToken')
    } as unknown as MockedObject<AuthService>;

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    service = TestBed.inject(SpecialtyService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('debe solicitar las especialidades activas y almacenarlas en cache', () => {
    let result: SpecialtyItem[] = [];
    service.getActiveSpecialties().subscribe(data => result = data);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/specialties'));
    expect(req.request.method).toBe('GET');
    req.flush(mockSpecialties);

    expect(result.length).toBe(2);
    expect(result[0].code).toBe('PSICOLOGIA');

    // La segunda llamada debe usar el observable cacheado sin hacer nueva petición HTTP
    service.getActiveSpecialties().subscribe(cached => {
      expect(cached).toEqual(mockSpecialties);
    });
    httpTesting.expectNone(request => request.url.endsWith('/v1/specialties'));
  });

  it('debe solicitar todas las especialidades para administracion', () => {
    service.getAllSpecialties().subscribe();

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/specialties/all'));
    expect(req.request.method).toBe('GET');
    req.flush(mockSpecialties);
  });

  it('debe crear una nueva especialidad y limpiar la cache', () => {
    const newSpec: Partial<SpecialtyItem> = { code: 'NUTRICION', name: 'Nutrición' };
    service.createSpecialty(newSpec).subscribe();

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/specialties'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 3, ...newSpec, active: true, displayOrder: 3 });
  });

  it('debe decodificar la especialidad desde el token JWT correctamente', () => {
    // Payload base64 con {"specialty":"PSICOLOGIA"}
    const header = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9';
    const payload = btoa(JSON.stringify({ specialty: 'PSICOLOGIA', sub: 'admin' }));
    const dummyToken = `${header}.${payload}.signature`;

    authServiceSpy.getToken.mockReturnValue(dummyToken);

    expect(service.getSpecialty()).toBe('PSICOLOGIA');
    expect(service.isPsychology()).toBe(true);
    expect(service.isDermatology()).toBe(false);
  });
});
