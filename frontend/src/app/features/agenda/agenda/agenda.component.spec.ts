import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { AgendaComponent } from './agenda.component';
import { AppointmentService } from '../../../core/services/appointment.service';
import { ScheduleBlockService } from '../../../core/services/schedule-block.service';
import { UserService } from '../../../core/services/user.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import { Appointment } from '../../../core/models/appointment.model';

describe('AgendaComponent', () => {
  let component: AgendaComponent;
  let fixture: ComponentFixture<AgendaComponent>;
  let appointmentService: MockedObject<AppointmentService>;
  let blockService: MockedObject<ScheduleBlockService>;
  let userService: MockedObject<UserService>;
  let viewPreferenceService: MockedObject<ViewPreferenceService>;

  beforeEach(async () => {
    localStorage.clear();

    const appointmentSpy = {
      search: vi.fn().mockName('AppointmentService.search'),
      updateStatus: vi.fn().mockName('AppointmentService.updateStatus'),
      create: vi.fn().mockName('AppointmentService.create'),
      update: vi.fn().mockName('AppointmentService.update')
    };
    appointmentSpy.search.mockReturnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
      numberOfElements: 0,
      first: true,
      last: true,
      empty: true
    }));

    const blockSpy = {
      getBlocks: vi.fn().mockName('ScheduleBlockService.getBlocks'),
      createBlock: vi.fn().mockName('ScheduleBlockService.createBlock'),
      deleteBlock: vi.fn().mockName('ScheduleBlockService.deleteBlock')
    };
    blockSpy.getBlocks.mockReturnValue(of([]));

    const userSpy = {
      getProfessionals: vi.fn().mockName('UserService.getProfessionals')
    };
    userSpy.getProfessionals.mockReturnValue(of([
      { id: 1, username: 'dr1', firstName: 'Juan', lastName: 'Pérez', specialty: 'PSICOLOGIA', enabled: true, roles: ['ROLE_ADMIN'] }
    ]));

    const viewPreferenceSpy = {
      getViewMode: vi.fn().mockName('ViewPreferenceService.getViewMode'),
      setViewMode: vi.fn().mockName('ViewPreferenceService.setViewMode'),
      isMobile: vi.fn().mockName('ViewPreferenceService.isMobile')
    };
    viewPreferenceSpy.getViewMode.mockReturnValue('list');
    viewPreferenceSpy.setViewMode.mockReturnValue(undefined);

    await TestBed.configureTestingModule({
      imports: [AgendaComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [
        { provide: AppointmentService, useValue: appointmentSpy },
        { provide: ScheduleBlockService, useValue: blockSpy },
        { provide: UserService, useValue: userSpy },
        { provide: ViewPreferenceService, useValue: viewPreferenceSpy }
      ]
    })
      .compileComponents();

    appointmentService = TestBed.inject(AppointmentService) as MockedObject<AppointmentService>;
    blockService = TestBed.inject(ScheduleBlockService) as MockedObject<ScheduleBlockService>;
    userService = TestBed.inject(UserService) as MockedObject<UserService>;
    viewPreferenceService = TestBed.inject(ViewPreferenceService) as MockedObject<ViewPreferenceService>;

    fixture = TestBed.createComponent(AgendaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  it('should use the compact list view returned by the preference service', () => {
    expect(component).toBeTruthy();
    expect(viewPreferenceService.getViewMode).toHaveBeenCalledWith('agenda_view_mode', 'calendar', 'list');
    expect(component.currentView).toBe('list');
    expect(component.professionals.length).toBe(1);
  });

  it('should keep dateRange form control enabled in calendar and list views', () => {
    expect(component.filterForm.get('dateRange')?.enabled).toBe(true);

    component.toggleView('list');
    expect(component.currentView).toBe('list');
    expect(component.filterForm.get('dateRange')?.enabled).toBe(true);

    component.toggleView('calendar');
    expect(component.currentView).toBe('calendar');
    expect(component.filterForm.get('dateRange')?.enabled).toBe(true);
  });

  it('should update appointments when dateRange filter changes to ALL (Cualquier fecha)', () => {
    vi.useFakeTimers();
    appointmentService.search.mockClear();

    component.filterForm.patchValue({ dateRange: 'ALL' });
    vi.advanceTimersByTime(350);

    expect(appointmentService.search).toHaveBeenCalled();
  });

  it('should update calendar week when dateRange changes in calendar view', () => {
    vi.useFakeTimers();
    component.toggleView('calendar');
    component.filterForm.patchValue({ dateRange: 'TODAY' });
    vi.advanceTimersByTime(350);

    expect(component.weekDays.length).toBe(7);
  });

  it('should format time range properly', () => {
    expect(component.formatTimeRange('09:00:00', '10:00:00')).toBe('09:00 – 10:00');
    expect(component.formatTimeRange('09:30', '10:15')).toBe('09:30 – 10:15');
    expect(component.formatTimeRange('09:00:00')).toBe('09:00');
    expect(component.formatTimeRange('')).toBe('');
  });

  it('should return appropriate dot class for each status', () => {
    expect(component.getStatusDotClass('PROGRAMADA')).toBe('bg-amber-500');
    expect(component.getStatusDotClass('CONFIRMADA')).toBe('bg-blue-500');
    expect(component.getStatusDotClass('COMPLETADA')).toBe('bg-emerald-500');
    expect(component.getStatusDotClass('CANCELADA')).toBe('bg-red-500');
    expect(component.getStatusDotClass('NO_ASISTIO')).toBe('bg-rose-400');
    expect(component.getStatusDotClass('OTHER')).toBe('bg-slate-400');
  });

  it('should toggle and close appointment action menu correctly', () => {
    expect(component.openMenuAppointmentId).toBeNull();

    const mockEvent = new MouseEvent('click');
    vi.spyOn(mockEvent, 'stopPropagation');

    component.toggleMenu(5, mockEvent);
    expect(component.openMenuAppointmentId).toBe(5);
    expect(mockEvent.stopPropagation).toHaveBeenCalled();

    component.toggleMenu(5);
    expect(component.openMenuAppointmentId).toBeNull();

    component.toggleMenu(8);
    expect(component.openMenuAppointmentId).toBe(8);

    component.onDocumentClick();
    expect(component.openMenuAppointmentId).toBeNull();

    component.toggleMenu(9);
    component.closeMenu();
    expect(component.openMenuAppointmentId).toBeNull();
  });

  it('should create a new appointment from a cancelled slot without retaining the patient', () => {
    const cancelledAppointment: Appointment = {
      id: 12,
      patientId: 44,
      appointmentDate: '2026-09-09',
      startTime: '10:00:00',
      endTime: '10:30:00',
      professionalId: 7,
      status: 'CANCELADA'
    };
    component.selectedAppointmentPreview = cancelledAppointment;
    component.isDrawerOpen = true;

    component.scheduleNewAppointmentInSameSlot(cancelledAppointment);

    expect(component.showForm).toBe(true);
    expect(component.appointmentToEdit).toBeNull();
    expect(component.initialAppointmentData).toEqual({
      appointmentDate: '2026-09-09',
      startTime: '10:00:00',
      endTime: '10:30:00',
      professionalId: 7
    });
    expect(component.initialAppointmentData?.patientId).toBeUndefined();
    expect(component.isDrawerOpen).toBe(false);
    expect(component.selectedAppointmentPreview).toBeNull();
  });

  it('should hide cancelled appointments from calendar slots without removing active appointments', () => {
    component.appointments = [
      {
        id: 12,
        patientId: 44,
        appointmentDate: '2026-09-09',
        startTime: '10:00:00',
        endTime: '10:30:00',
        status: 'CANCELADA'
      },
      {
        id: 13,
        patientId: 45,
        appointmentDate: '2026-09-09',
        startTime: '10:00:00',
        endTime: '10:30:00',
        status: 'PROGRAMADA'
      }
    ];

    const visibleAppointments = component.getAppointmentsForDayAndHour(new Date(2026, 8, 9), '10:00');

    expect(visibleAppointments.map(appointment => appointment.id)).toEqual([13]);
  });
});
