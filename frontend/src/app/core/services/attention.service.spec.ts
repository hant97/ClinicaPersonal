import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AttentionService } from './attention.service';
import { Attention, AttentionSummary } from '../models/attention.model';
import { PageResponse } from '../models/page.model';

describe('AttentionService', () => {
  let service: AttentionService;
  let httpTesting: HttpTestingController;

  const mockPageResponse: PageResponse<Attention> = {
    content: [
      {
        id: 1,
        patientId: 10,
        patientName: 'Maria Lopez',
        specialty: 'PSICOLOGIA',
        attentionDate: '2026-08-20',
        status: 'EN_PROCESO',
        startTime: '10:00:00'
      }
    ],
    page: {
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0
    }
  };

  const mockSummary: AttentionSummary = {
    totalToday: 5,
    scheduledToday: 2,
    inProgressToday: 1,
    attendedToday: 1,
    paidToday: 1,
    cancelledToday: 0,
    pendingBillingAmount: 150.0
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AttentionService
      ]
    });

    service = TestBed.inject(AttentionService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('debe obtener atenciones con filtros y paginación', () => {
    let result: PageResponse<Attention> | undefined;
    service.getAll('Maria', 'EN_PROCESO', 5, undefined, '2026-08-01', '2026-08-20', 0, 20)
      .subscribe(data => result = data);

    const req = httpTesting.expectOne(request =>
      request.url.endsWith('/v1/attentions') &&
      request.params.get('searchTerm') === 'Maria' &&
      request.params.get('status') === 'EN_PROCESO' &&
      request.params.get('professionalId') === '5'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPageResponse);

    expect(result).toBeDefined();
    expect(result?.content.length).toBe(1);
    expect(result?.content[0].patientName).toBe('Maria Lopez');
  });

  it('debe obtener el resumen de KPIs de hoy', () => {
    let summary: AttentionSummary | undefined;
    service.getTodaySummary().subscribe(data => summary = data);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/attentions/today-summary'));
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);

    expect(summary).toBeDefined();
    expect(summary?.totalToday).toBe(5);
    expect(summary?.pendingBillingAmount).toBe(150.0);
  });

  it('debe crear una atención rápida', () => {
    const payload: Partial<Attention> = {
      patientId: 10,
      attentionDate: '2026-08-20',
      status: 'EN_PROCESO',
      motive: 'Urgencia'
    };

    let created: Attention | undefined;
    service.create(payload).subscribe(data => created = data);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/attentions'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 99, ...payload });

    expect(created?.id).toBe(99);
  });

  it('debe iniciar una atención desde una cita', () => {
    let result: Attention | undefined;
    service.createFromAppointment(42).subscribe(data => result = data);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/attentions/from-appointment/42'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 101, appointmentId: 42, status: 'EN_PROCESO' });

    expect(result?.id).toBe(101);
    expect(result?.appointmentId).toBe(42);
  });

  it('debe actualizar el estado de una atención', () => {
    let updated: Attention | undefined;
    service.updateStatus(101, 'ATENDIDA', 'Consulta finalizada').subscribe(data => updated = data);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/attentions/101/status'));
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.status).toBe('ATENDIDA');
    expect(req.request.body.notes).toBe('Consulta finalizada');
    req.flush({ id: 101, status: 'ATENDIDA' });

    expect(updated?.status).toBe('ATENDIDA');
  });

  it('debe eliminar una atención', () => {
    let completed = false;
    service.delete(101).subscribe(() => completed = true);

    const req = httpTesting.expectOne(request => request.url.endsWith('/v1/attentions/101'));
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(completed).toBeTrue();
  });
});
