import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AttentionListComponent } from './attention-list.component';
import { AttentionService } from '../../../core/services/attention.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { PageResponse } from '../../../core/models/page.model';
import { Attention, AttentionSummary } from '../../../core/models/attention.model';

describe('AttentionListComponent', () => {
  let component: AttentionListComponent;
  let fixture: ComponentFixture<AttentionListComponent>;
  let attentionServiceSpy: jasmine.SpyObj<AttentionService>;
  let patientServiceSpy: jasmine.SpyObj<PatientService>;
  let clinicalServiceSpy: jasmine.SpyObj<ClinicalServiceService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let router: Router;

  const mockPageResponse: PageResponse<Attention> = {
    content: [
      {
        id: 1,
        patientId: 10,
        patientName: 'Ana Ruiz',
        patientDocumentNumber: '76543210',
        specialty: 'PSICOLOGIA',
        attentionDate: '2026-08-20',
        status: 'AGENDADA',
        startTime: '10:00:00'
      }
    ],
    page: {
      totalElements: 1,
      totalPages: 1,
      size: 15,
      number: 0
    }
  };

  const mockSummary: AttentionSummary = {
    totalToday: 3,
    scheduledToday: 1,
    inProgressToday: 1,
    attendedToday: 1,
    paidToday: 0,
    cancelledToday: 0,
    pendingBillingAmount: 120.0
  };

  beforeEach(async () => {
    attentionServiceSpy = jasmine.createSpyObj('AttentionService', [
      'getAll',
      'getTodaySummary',
      'create',
      'updateStatus',
      'delete'
    ]);
    patientServiceSpy = jasmine.createSpyObj('PatientService', ['search']);
    clinicalServiceSpy = jasmine.createSpyObj('ClinicalServiceService', ['getAllActiveServices']);
    userServiceSpy = jasmine.createSpyObj('UserService', ['getCurrentUserProfile']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['show']);

    attentionServiceSpy.getAll.and.returnValue(of(mockPageResponse));
    attentionServiceSpy.getTodaySummary.and.returnValue(of(mockSummary));
    clinicalServiceSpy.getAllActiveServices.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AttentionListComponent, RouterTestingModule],
      providers: [
        { provide: AttentionService, useValue: attentionServiceSpy },
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: ClinicalServiceService, useValue: clinicalServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: {} }, queryParams: of({}) } }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(AttentionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse y cargar resumen y lista de atenciones', () => {
    expect(component).toBeTruthy();
    expect(component.attentions.length).toBe(1);
    expect(component.summary?.totalToday).toBe(3);
    expect(attentionServiceSpy.getAll).toHaveBeenCalled();
    expect(attentionServiceSpy.getTodaySummary).toHaveBeenCalled();
  });

  it('debe iniciar la atención al invocar startAttention', () => {
    const attention = mockPageResponse.content[0];
    const updatedAttention: Attention = { ...attention, status: 'EN_PROCESO' };
    attentionServiceSpy.updateStatus.and.returnValue(of(updatedAttention));

    component.startAttention(attention);

    expect(attentionServiceSpy.updateStatus).toHaveBeenCalledWith(1, 'EN_PROCESO');
    expect(toastServiceSpy.show).toHaveBeenCalledWith('Atención iniciada en consulta', 'success');
  });

  it('debe filtrar por estado al invocar setStatusFilter', () => {
    component.setStatusFilter('EN_PROCESO');
    expect(component.statusFilter).toBe('EN_PROCESO');
    expect(attentionServiceSpy.getAll).toHaveBeenCalled();
  });

  it('debe abrir y cerrar modal de captura rápida', () => {
    component.openQuickModal();
    expect(component.showQuickModal).toBeTrue();
    component.closeQuickModal();
    expect(component.showQuickModal).toBeFalse();
  });

  it('debe navegar a sesión clínica al invocar goToClinicalSession', () => {
    const attention = mockPageResponse.content[0];
    component.goToClinicalSession(attention);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/patients/10/sessions/new'],
      jasmine.objectContaining({
        queryParams: jasmine.objectContaining({ attentionId: 1 })
      })
    );
  });
});
