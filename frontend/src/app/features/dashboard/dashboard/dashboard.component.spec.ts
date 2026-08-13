import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { DashboardService, DashboardStats } from '../../../core/services/dashboard.service';
import { DashboardComponent } from './dashboard.component';
import { RiskAlertService } from '../../../core/services/risk-alert.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { provideRouter } from '@angular/router';

const dashboardStats: DashboardStats = {
  activePatients: 12, appointmentsToday: 3, monthlyIncome: 450,
  upcomingAppointments: [], attendanceRate: 90, cancelledAppointments: 0,
  newPatientsThisMonth: 2, monthlyIncomeGrowth: 10,
   activeRiskAlerts: [], lowStockSupplies: [],
   psychometricEvaluationsThisMonth: 0,
   dermatologicalEvaluationsThisMonth: 0,
   dermatologicalProceduresThisMonth: 0
};

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardService: jasmine.SpyObj<DashboardService>;

  beforeEach(async () => {
    dashboardService = jasmine.createSpyObj<DashboardService>('DashboardService', ['getDashboardStats']);
    dashboardService.getDashboardStats.and.returnValue(of(dashboardStats));
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: dashboardService },
        { provide: SpecialtyService, useValue: { isPsychology: () => false, isDermatology: () => true } },
        { provide: RiskAlertService, useValue: { getActiveRiskAlerts: () => of({ content: [], totalElements: 0 }) } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('muestra el resumen cuando la carga es correcta', () => {
    fixture.detectChanges();
    expect(component.stats).toEqual(dashboardStats);
    expect(fixture.nativeElement.textContent).toContain('Pacientes Activos');
  });

  it('muestra una opción para reintentar cuando ocurre un error', () => {
    dashboardService.getDashboardStats.and.returnValue(throwError(() => new Error('Error de red')));
    fixture.detectChanges();
    expect(component.loadError).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Reintentar');
  });
});
