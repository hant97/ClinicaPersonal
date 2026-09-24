import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ClinicalHistoryPrintComponent } from './clinical-history-print.component';
import { ClinicalHistoryService } from '../../../core/services/clinical-history.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { DermatologicalEvaluationService } from '../../../core/services/dermatological-evaluation.service';
import { ClinicSettingsService, ClinicSettings } from '../../../core/services/clinic-settings.service';
import { UserService } from '../../../core/services/user.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { PageResponse } from '../../../core/models/page.model';

describe('ClinicalHistoryPrintComponent', () => {
  let component: ClinicalHistoryPrintComponent;
  let fixture: ComponentFixture<ClinicalHistoryPrintComponent>;
  let clinicalHistoryServiceMock: any;
  let patientServiceMock: any;
  let sessionServiceMock: any;
  let dermatologicalEvaluationServiceMock: any;
  let clinicSettingsServiceMock: any;
  let userServiceMock: any;
  let specialtyServiceMock: any;

  const mockSettings: ClinicSettings = {
    id: 1,
    clinicName: 'Clínica Vida Saludable',
    shortName: 'Vida',
    logoUrl: '',
    contactEmail: 'contacto@vida.com',
    contactPhone: '999999999',
    address: 'Av. Principal 123'
  };

  const emptyPage: PageResponse<any> = {
    content: [],
    page: { number: 0, size: 10, totalElements: 0, totalPages: 0 }
  };

  beforeEach(async () => {
    clinicalHistoryServiceMock = {
      get: vi.fn().mockReturnValue(of({
        patientId: 42,
        specialty: 'DERMATOLOGIA',
        generalHistory: { patientId: 42, pathologicalHistory: 'Ninguno' },
        allergies: [{ id: 1, patientId: 42, allergen: 'Penicilina', active: true }],
        medications: [],
        diagnoses: []
      }))
    };

    patientServiceMock = {
      getById: vi.fn().mockReturnValue(of({
        id: 42,
        firstName: 'Ana',
        lastName: 'Pérez',
        identificationDocument: '12345678',
        dateOfBirth: '1990-01-01'
      }))
    };

    sessionServiceMock = {
      getSessionsByPatientId: vi.fn().mockReturnValue(of({
        content: [
          {
            id: 1,
            patientId: 42,
            sessionDate: '2026-08-01',
            startTime: '10:00',
            endTime: '11:00',
            sessionType: 'CONTROL',
            modality: 'PRESENCIAL',
            status: 'COMPLETADA',
            isConfidential: false
          }
        ],
        page: { number: 0, size: 10, totalElements: 1, totalPages: 1 }
      }))
    };

    dermatologicalEvaluationServiceMock = {
      getByPatientId: vi.fn().mockReturnValue(of(emptyPage))
    };

    clinicSettingsServiceMock = {
      settings$: of(mockSettings),
      loadSettings: vi.fn(),
      getLogoUrl: vi.fn().mockImplementation((path: string) => `http://localhost:8080${path}`)
    };

    userServiceMock = {
      getCurrentUserProfile: vi.fn().mockReturnValue(of({ id: 1, username: 'dra', firstName: 'Laura', lastName: 'Rojas' }))
    };

    specialtyServiceMock = {
      isPsychology: () => false,
      isDermatology: () => true
    };

    await TestBed.configureTestingModule({
      imports: [ClinicalHistoryPrintComponent],
      providers: [
        { provide: ClinicalHistoryService, useValue: clinicalHistoryServiceMock },
        { provide: PatientService, useValue: patientServiceMock },
        { provide: ClinicalSessionService, useValue: sessionServiceMock },
        { provide: DermatologicalEvaluationService, useValue: dermatologicalEvaluationServiceMock },
        { provide: ClinicSettingsService, useValue: clinicSettingsServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: SpecialtyService, useValue: specialtyServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicalHistoryPrintComponent);
    component = fixture.componentInstance;
    component.patientId = 42;
  });

  it('should create and load the clinical history data', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.loading).toBe(false);
    expect(clinicalHistoryServiceMock.get).toHaveBeenCalledWith(42);
    expect(patientServiceMock.getById).toHaveBeenCalledWith(42);
    expect(component.patient?.firstName).toBe('Ana');
    expect(component.history?.allergies?.length).toBe(1);
    expect(component.sessions.length).toBe(1);
  });

  it('should fetch dermatological evaluations only for dermatology', () => {
    fixture.detectChanges();

    expect(dermatologicalEvaluationServiceMock.getByPatientId).toHaveBeenCalledWith(42, 0, 100);
  });

  it('should render patient name and clinic name in the document', () => {
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Ana Pérez');
    expect(text).toContain('Clínica Vida Saludable');
    expect(text).toContain('Historia Clínica');
  });

  it('should call window.print when print is triggered', () => {
    fixture.detectChanges();
    vi.spyOn(window, 'print');

    component.print();

    expect(window.print).toHaveBeenCalled();
  });

  it('should emit close when onClose is called', () => {
    vi.spyOn(component.close, 'emit');

    component.onClose();

    expect(component.close.emit).toHaveBeenCalled();
  });
});
