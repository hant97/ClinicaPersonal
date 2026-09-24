import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { AppointmentFormComponent } from './appointment-form.component';
import { AppointmentService } from '../../../core/services/appointment.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';

describe('AppointmentFormComponent', () => {
  let component: AppointmentFormComponent;
  let fixture: ComponentFixture<AppointmentFormComponent>;
  let appointmentService: MockedObject<AppointmentService>;
  let catalogService: MockedObject<CatalogService>;
  let clinicalService: MockedObject<ClinicalServiceService>;
  let userService: MockedObject<UserService>;

  beforeEach(async () => {
    const appointmentSpy = {
      search: vi.fn().mockName('AppointmentService.search'),
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
    appointmentSpy.create.mockReturnValue(of({ id: 1, patientId: 1, appointmentDate: '2026-10-10', startTime: '10:00', endTime: '10:30', status: 'PROGRAMADA' }));

    const catalogSpy = {
      getActiveItemsByCatalogCode: vi.fn().mockName('CatalogService.getActiveItemsByCatalogCode')
    };
    catalogSpy.getActiveItemsByCatalogCode.mockReturnValue(of([
      { id: 1, catalogCode: 'APPOINTMENT_MODALITY', itemCode: 'PRESENCIAL', itemName: 'Presencial', active: true }
    ]));

    const clinicalSpy = {
      getAllActiveServices: vi.fn().mockName('ClinicalServiceService.getAllActiveServices')
    };
    clinicalSpy.getAllActiveServices.mockReturnValue(of([
      { id: 1, name: 'Consulta General', price: 100, durationMinutes: 45, active: true }
    ]));

    const userSpy = {
      getProfessionals: vi.fn().mockName('UserService.getProfessionals'),
      getCurrentUserProfile: vi.fn().mockName('UserService.getCurrentUserProfile')
    };
    userSpy.getProfessionals.mockReturnValue(of([
      { id: 1, username: 'dr1', firstName: 'Juan', lastName: 'Pérez', specialty: 'PSICOLOGIA', enabled: true, roles: ['ROLE_ADMIN'] }
    ]));
    userSpy.getCurrentUserProfile.mockReturnValue(of({
      id: 1,
      username: 'dr1',
      firstName: 'Juan',
      lastName: 'Pérez',
      specialty: 'PSICOLOGIA',
      roles: ['ROLE_ADMIN'],
      enabled: true
    }));

    await TestBed.configureTestingModule({
      imports: [AppointmentFormComponent, HttpClientTestingModule],
      providers: [
        { provide: AppointmentService, useValue: appointmentSpy },
        { provide: CatalogService, useValue: catalogSpy },
        { provide: ClinicalServiceService, useValue: clinicalSpy },
        { provide: UserService, useValue: userSpy },
        ToastService,
        NotificationService
      ]
    }).compileComponents();

    appointmentService = TestBed.inject(AppointmentService) as MockedObject<AppointmentService>;
    catalogService = TestBed.inject(CatalogService) as MockedObject<CatalogService>;
    clinicalService = TestBed.inject(ClinicalServiceService) as MockedObject<ClinicalServiceService>;
    userService = TestBed.inject(UserService) as MockedObject<UserService>;

    fixture = TestBed.createComponent(AppointmentFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create form and load catalogs and professionals', () => {
    expect(component).toBeTruthy();
    expect(component.professionals.length).toBe(1);
    expect(component.appointmentModalities.length).toBe(1);
    expect(component.clinicalServices.length).toBe(1);
  });

  it('should generate projected recurring dates preview', () => {
    component.appointmentForm.patchValue({
      appointmentDate: '2026-09-01',
      recurrenceCount: 3
    });
    const preview = component.getRecurringDatesPreview();
    expect(preview.length).toBe(3);
    expect(preview[0]).toContain('Sesión 1');
    expect(preview[1]).toContain('Sesión 2');
    expect(preview[2]).toContain('Sesión 3');
  });

  it('should auto-select current user if user is a professional', () => {
    expect(component.appointmentForm.get('professionalId')?.value).toBe(1);
  });

  it('should format professional display name with fallback to username', () => {
    const profWithNames = { id: 1, username: 'dr1', firstName: 'Juan', lastName: 'Pérez', specialty: 'DERMATOLOGIA' };
    expect(component.getProfessionalDisplayName(profWithNames)).toBe('Juan Pérez (DERMATOLOGIA)');

    const profWithoutNames = { id: 2, username: 'admin', firstName: '', lastName: '', specialty: 'DERMATOLOGIA' };
    expect(component.getProfessionalDisplayName(profWithoutNames)).toBe('admin (DERMATOLOGIA)');
  });

  it('should submit appointment successfully when valid', () => {
    component.appointmentForm.patchValue({
      patientId: 1,
      appointmentDate: '2026-10-15',
      startTime: '09:00',
      endTime: '09:30',
      status: 'PROGRAMADA',
      modality: 'PRESENCIAL',
      professionalId: 1
    });

    vi.spyOn(component.saved, 'emit');
    component.onSubmit();

    expect(appointmentService.create).toHaveBeenCalled();
    expect(component.saved.emit).toHaveBeenCalled();
  });
});
