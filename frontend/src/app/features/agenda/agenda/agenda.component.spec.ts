import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { AgendaComponent } from './agenda.component';
import { AppointmentService } from '../../../core/services/appointment.service';

describe('AgendaComponent', () => {
  let component: AgendaComponent;
  let fixture: ComponentFixture<AgendaComponent>;
  let appointmentService: jasmine.SpyObj<AppointmentService>;

  beforeEach(async () => {
    const appointmentSpy = jasmine.createSpyObj('AppointmentService', ['search', 'updateStatus', 'create', 'update']);
    appointmentSpy.search.and.returnValue(of({
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

    await TestBed.configureTestingModule({
      imports: [AgendaComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [
        { provide: AppointmentService, useValue: appointmentSpy }
      ]
    })
    .compileComponents();

    appointmentService = TestBed.inject(AppointmentService) as jasmine.SpyObj<AppointmentService>;

    fixture = TestBed.createComponent(AgendaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and default to calendar view', () => {
    expect(component).toBeTruthy();
    expect(component.currentView).toBe('calendar');
  });

  it('should keep dateRange form control enabled in calendar and list views', () => {
    expect(component.filterForm.get('dateRange')?.enabled).toBeTrue();

    component.toggleView('list');
    expect(component.currentView).toBe('list');
    expect(component.filterForm.get('dateRange')?.enabled).toBeTrue();

    component.toggleView('calendar');
    expect(component.currentView).toBe('calendar');
    expect(component.filterForm.get('dateRange')?.enabled).toBeTrue();
  });

  it('should update appointments when dateRange filter changes to ALL (Cualquier fecha)', fakeAsync(() => {
    appointmentService.search.calls.reset();

    component.filterForm.patchValue({ dateRange: 'ALL' });
    tick(350);

    expect(appointmentService.search).toHaveBeenCalled();
  }));

  it('should update calendar week when dateRange changes in calendar view', fakeAsync(() => {
    const today = new Date();
    component.filterForm.patchValue({ dateRange: 'TODAY' });
    tick(350);

    expect(component.weekDays.length).toBe(7);
  }));

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
    spyOn(mockEvent, 'stopPropagation');

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
});

