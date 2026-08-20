import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuditLogService } from './audit-log.service';
import { AuditLog } from '../models/audit-log.model';
import { PageResponse } from '../models/page.model';

describe('AuditLogService', () => {
  let service: AuditLogService;
  let httpTesting: HttpTestingController;

  const mockResponse: PageResponse<AuditLog> = {
    content: [
      {
        id: 1,
        userId: 10,
        username: 'admin',
        specialty: 'PSICOLOGIA',
        action: 'CREATE',
        entityType: 'PATIENT',
        entityId: '55',
        detail: 'Nuevo paciente',
        ip: '127.0.0.1',
        createdAt: '2026-08-20T10:00:00'
      }
    ],
    page: {
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AuditLogService
      ]
    });

    service = TestBed.inject(AuditLogService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('debe obtener registros de auditoria con filtros y paginacion', () => {
    let result: PageResponse<AuditLog> | undefined;
    service.getAuditLogs({
      startDate: '2026-08-01',
      endDate: '2026-08-20',
      action: 'CREATE',
      entityType: 'PATIENT',
      username: 'admin',
      page: 0,
      size: 20
    }).subscribe(data => result = data);

    const req = httpTesting.expectOne(request =>
      request.url.endsWith('/v1/audit-logs') &&
      request.params.get('action') === 'CREATE' &&
      request.params.get('entityType') === 'PATIENT' &&
      request.params.get('username') === 'admin'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);

    expect(result).toBeDefined();
    expect(result?.content.length).toBe(1);
    expect(result?.content[0].action).toBe('CREATE');
  });

  it('debe obtener las acciones disponibles', () => {
    let actions: string[] = [];
    service.getDistinctActions().subscribe(data => actions = data);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/audit-logs/actions'));
    expect(req.request.method).toBe('GET');
    req.flush(['CREATE', 'DELETE', 'LOGIN', 'UPDATE']);

    expect(actions.length).toBe(4);
    expect(actions).toContain('LOGIN');
  });

  it('debe obtener los tipos de entidad disponibles', () => {
    let entityTypes: string[] = [];
    service.getDistinctEntityTypes().subscribe(data => entityTypes = data);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/audit-logs/entity-types'));
    expect(req.request.method).toBe('GET');
    req.flush(['AUTH', 'PATIENT', 'PAYMENT', 'USER']);

    expect(entityTypes.length).toBe(4);
    expect(entityTypes).toContain('PATIENT');
  });
});
