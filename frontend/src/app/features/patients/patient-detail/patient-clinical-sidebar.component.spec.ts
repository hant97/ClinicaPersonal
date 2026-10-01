import { TestBed } from '@angular/core/testing';
import { Appointment } from '../../../core/models/appointment.model';
import { PatientClinicalSidebarComponent } from './patient-clinical-sidebar.component';

describe('PatientClinicalSidebarComponent', () => {
  it('keeps patient identity out of the clinical sidebar and hides clinical widgets for nonprofessionals', async () => {
    await TestBed.configureTestingModule({ imports: [PatientClinicalSidebarComponent] }).compileComponents();
    const fixture = TestBed.createComponent(PatientClinicalSidebarComponent);
    fixture.componentInstance.patient = {
      id: 42,
      firstName: 'Ana',
      lastName: 'Pérez',
      identificationDocument: '12345678',
      contactNumber: '999999999'
    } as PatientClinicalSidebarComponent['patient'];
    fixture.componentInstance.activeAlerts = [{ id: 1, type: 'ALERGIA', level: 'ALTA', description: 'Penicilina' }] as PatientClinicalSidebarComponent['activeAlerts'];
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Ana Pérez');
    expect(fixture.nativeElement.textContent).not.toContain('12345678');
    expect(fixture.nativeElement.textContent).not.toContain('Alertas Activas');
    expect(fixture.nativeElement.textContent).not.toContain('999999999');

    fixture.componentInstance.isProfessional = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Alertas Activas');
  });

  it('renders appointment dates, time ranges, service names and readable statuses', async () => {
    await TestBed.configureTestingModule({ imports: [PatientClinicalSidebarComponent] }).compileComponents();
    const fixture = TestBed.createComponent(PatientClinicalSidebarComponent);
    fixture.componentInstance.patient = { id: 42 } as PatientClinicalSidebarComponent['patient'];
    const appointment: Appointment = {
      id: 1,
      patientId: 42,
      appointmentDate: '2030-05-06',
      startTime: '09:00:00',
      endTime: '09:30:00',
      status: 'CONFIRMADA',
      clinicalServiceName: 'Control dermatológico'
    };
    fixture.componentInstance.upcomingAppointments = [
      appointment,
      { ...appointment, id: 2, clinicalServiceName: undefined, status: 'PROGRAMADA' },
      { ...appointment, id: 3, clinicalServiceName: 'Tercera cita' }
    ];
    fixture.componentInstance.recentAppointments = [
      { ...appointment, id: 4, appointmentDate: '2030-05-01', status: 'COMPLETADA' }
    ];
    fixture.detectChanges();

    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('06/05/2030');
    expect(text).toContain('09:00–09:30');
    expect(text).toContain('Control dermatológico');
    expect(text).toContain('Consulta clínica');
    expect(text).toContain('Confirmada');
    expect(text).toContain('Programada');
    expect(text).not.toContain('Tercera cita');
    expect(text).toContain('Últimas visitas');
    expect(text).toContain('01/05/2030');
    expect(text).toContain('Completada');
  });

  describe('appointment labels', () => {
    let component: PatientClinicalSidebarComponent;

    beforeEach(() => {
      vi.useFakeTimers({ toFake: ['Date'] });
      vi.setSystemTime(new Date(2026, 11, 31, 23, 30));
      component = new PatientClinicalSidebarComponent();
    });

    afterEach(() => vi.useRealTimers());

    it('uses local calendar dates for today and tomorrow across a year boundary', () => {
      expect(component.appointmentDateLabel('2026-12-31')).toBe('Hoy');
      expect(component.appointmentDateLabel('2027-01-01')).toBe('Mañana');
      expect(component.appointmentDateLabel('2027-01-02')).toBe('02/01/2027');
    });

    it.each(['', 'invalid', '2026-02-29', '2026-13-01', '2026-04-31'])(
      'preserves the invalid date %j instead of displaying a different day',
      value => expect(component.appointmentDateLabel(value)).toBe(value)
    );

    it('formats valid leap days', () => {
      expect(component.appointmentDateLabel('2028-02-29')).toBe('29/02/2028');
    });

    it.each([
      ['PROGRAMADA', 'Programada'],
      ['confirmada', 'Confirmada'],
      ['COMPLETADA', 'Completada'],
      ['CANCELADA', 'Cancelada'],
      ['NO_ASISTIO', 'No asistió'],
      ['EN_ESPERA', 'En espera'],
      ['', 'Sin estado']
    ])('displays %j as %j', (status, label) => {
      expect(component.appointmentStatusLabel(status)).toBe(label);
    });
  });
});
