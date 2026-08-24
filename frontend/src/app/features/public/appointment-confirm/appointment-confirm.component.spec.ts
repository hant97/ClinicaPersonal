import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, Subject } from 'rxjs';
import { PublicAppointmentConfirmation } from '../../../core/models/appointment.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AppointmentConfirmComponent } from './appointment-confirm.component';

describe('AppointmentConfirmComponent', () => {
  let fixture: ComponentFixture<AppointmentConfirmComponent>;
  let component: AppointmentConfirmComponent;
  let appointmentService: jasmine.SpyObj<AppointmentService>;

  const preview: PublicAppointmentConfirmation = {
    confirmed: false,
    alreadyConfirmed: false,
    confirmable: true,
    message: 'Pendiente de confirmación',
    patientName: 'Ana Gómez',
    appointmentDate: '2026-09-01',
    startTime: '10:00:00',
    clinicName: 'Clínica'
  };

  beforeEach(async () => {
    appointmentService = jasmine.createSpyObj<AppointmentService>(
      'AppointmentService',
      ['getConfirmation', 'confirm']
    );
    appointmentService.getConfirmation.and.returnValue(of(preview));

    await TestBed.configureTestingModule({
      imports: [AppointmentConfirmComponent],
      providers: [
        { provide: AppointmentService, useValue: appointmentService },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ token: 'token-123' })) }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppointmentConfirmComponent);
    component = fixture.componentInstance;
  });

  it('solo consulta la cita al abrir la pantalla', () => {
    fixture.detectChanges();

    expect(appointmentService.getConfirmation).toHaveBeenCalledOnceWith('token-123');
    expect(appointmentService.confirm).not.toHaveBeenCalled();
    expect(component.result).toEqual(preview);
  });

  it('confirma únicamente tras una acción explícita', () => {
    const confirmed: PublicAppointmentConfirmation = {
      ...preview,
      confirmed: true,
      confirmable: false,
      message: 'Cita confirmada exitosamente.'
    };
    appointmentService.confirm.and.returnValue(of(confirmed));
    fixture.detectChanges();

    component.confirm();

    expect(appointmentService.confirm).toHaveBeenCalledOnceWith('token-123');
    expect(component.result).toEqual(confirmed);
  });

  it('bloquea dobles confirmaciones mientras la solicitud está en curso', () => {
    const pending = new Subject<PublicAppointmentConfirmation>();
    appointmentService.confirm.and.returnValue(pending.asObservable());
    fixture.detectChanges();

    component.confirm();
    component.confirm();

    expect(appointmentService.confirm).toHaveBeenCalledTimes(1);
    expect(component.submitting).toBeTrue();
    pending.complete();
  });
});
