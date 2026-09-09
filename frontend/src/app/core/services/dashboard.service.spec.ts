import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardService, DashboardStats } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DashboardService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('excluye las citas canceladas de la agenda y del contador de hoy', () => {
    const response: DashboardStats = {
      activePatients: 2,
      appointmentsToday: 2,
      monthlyIncome: 0,
      upcomingAppointments: [],
      todaysAppointments: [
        {
          id: 1,
          patientId: 10,
          appointmentDate: '2026-09-09',
          startTime: '10:00:00',
          endTime: '10:30:00',
          status: 'CANCELADA'
        },
        {
          id: 2,
          patientId: 11,
          appointmentDate: '2026-09-09',
          startTime: '10:00:00',
          endTime: '10:30:00',
          status: 'CONFIRMADA'
        }
      ],
      attendanceRate: 0,
      cancelledAppointments: 1,
      newPatientsThisMonth: 0,
      monthlyIncomeGrowth: 0,
      activeRiskAlerts: [],
      lowStockSupplies: [],
      pendingSoapNotes: [],
      psychometricEvaluationsThisMonth: 0,
      dermatologicalEvaluationsThisMonth: 0,
      dermatologicalProceduresThisMonth: 0
    };

    service.getDashboardStats().subscribe(stats => {
      expect(stats.appointmentsToday).toBe(1);
      expect(stats.todaysAppointments.map(appointment => appointment.id)).toEqual([2]);
      expect(stats.todaysAppointments[0].status).toBe('Confirmada');
    });

    const request = httpTesting.expectOne(req => req.url.endsWith('/v1/dashboard/stats'));
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });
});
