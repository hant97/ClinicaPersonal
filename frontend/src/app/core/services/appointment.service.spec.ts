import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PublicAppointmentConfirmation } from '../models/appointment.model';
import { AppointmentService } from './appointment.service';

describe('AppointmentService', () => {
  let service: AppointmentService;
  let httpTesting: HttpTestingController;

  const response: PublicAppointmentConfirmation = {
    confirmed: false,
    alreadyConfirmed: false,
    confirmable: true,
    message: 'Pendiente de confirmación'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AppointmentService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AppointmentService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('consulta el token sin modificar la cita', () => {
    service.getConfirmation('token-123').subscribe(result => expect(result).toEqual(response));

    const request = httpTesting.expectOne(req =>
      req.url.endsWith('/v1/public/appointments/confirm/token-123'));
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('confirma mediante POST sin cuerpo', () => {
    service.confirm('token-123').subscribe(result => expect(result).toEqual(response));

    const request = httpTesting.expectOne(req =>
      req.url.endsWith('/v1/public/appointments/confirm/token-123'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush(response);
  });
});
