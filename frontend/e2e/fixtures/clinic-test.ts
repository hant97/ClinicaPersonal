import { expect, Page, Route, test as base } from '@playwright/test';

const today = new Date().toISOString().slice(0, 10);

const authResponse = {
  token: 'e30.eyJzcGVjaWFsdHkiOiJQU0lDT0xPR0lBIn0.e2e-signature',
  specialty: 'PSICOLOGIA',
  roles: ['ROLE_PROFESIONAL', 'ROLE_ADMIN'],
};

const professional = {
  id: 1,
  username: 'admin.e2e',
  firstName: 'Alejandra Nombre Profesional Extenso',
  lastName: 'Salazar Apellido Compuesto',
  email: 'alejandra.salazar+responsive@clinica-e2e.example',
  phone: '999999999',
  roles: ['ROLE_PROFESIONAL', 'ROLE_ADMIN'],
  specialty: 'PSICOLOGIA',
  enabled: true,
};

const patient = {
  id: 1,
  uuid: 'patient-e2e-001',
  firstName: 'María Nombre de Paciente Particularmente Extenso',
  lastName: 'Fernández de la Cruz',
  identificationDocument: '12345678',
  documentType: 'DNI',
  dateOfBirth: '1990-01-01',
  contactNumber: '988888888',
  email: 'paciente.con.correo.extenso+responsive@dominio-clinico.example',
  active: true,
  specialty: 'PSICOLOGIA',
};

const appointment = {
  id: 1,
  patientId: patient.id,
  patientUuid: patient.uuid,
  patientName: `${patient.firstName} ${patient.lastName}`,
  patientEmail: patient.email,
  patientPhone: patient.contactNumber,
  appointmentDate: today,
  startTime: '10:00:00',
  endTime: '11:00:00',
  status: 'CONFIRMADA',
  modality: 'PRESENCIAL',
  professionalId: professional.id,
  professionalName: `${professional.firstName} ${professional.lastName}`,
  clinicalServiceId: 1,
  clinicalServiceName: 'Evaluación psicológica integral con nombre extenso',
  notes: 'Motivo de consulta deliberadamente extenso para validar el ajuste responsive.',
  isPaid: false,
};

const payment = {
  id: 1,
  patientId: patient.id,
  patientName: `${patient.firstName} ${patient.lastName}`,
  amount: 987654.32,
  paidAmount: 123456.78,
  balanceAmount: 864197.54,
  paymentDate: `${today}T10:30:00`,
  paymentMethod: 'TRANSFERENCIA',
  description: 'Evaluación clínica especializada con descripción particularmente extensa',
  status: 'PARCIAL',
  items: [],
  transactions: [],
};

const supply = {
  id: 1,
  name: 'Guantes de nitrilo extralargos para procedimientos especializados',
  description: 'Insumo clínico de prueba para contenido extremo.',
  currentStock: 123456,
  minStockLevel: 10,
  unit: 'CAJAS',
  price: 987654.32,
  expirationDate: '2027-12-31',
  specialty: 'PSICOLOGIA',
};

const clinicalService = {
  id: 1,
  name: 'Evaluación psicológica integral con nombre extenso',
  description: 'Servicio clínico utilizado por las pruebas responsive.',
  price: 987654.32,
  category: 'EVALUACION',
  durationMinutes: 60,
  active: true,
  specialty: 'PSICOLOGIA',
};

const catalogItem = {
  id: 1,
  catalogId: 1,
  itemCode: 'ESTADO_CLINICO_MUY_EXTENSO',
  itemName: 'Nombre de opción clínica deliberadamente largo para validar su ajuste',
  isActive: true,
  orderIndex: 0,
};

const catalog = {
  id: 1,
  code: 'ESTADOS_E2E',
  name: 'Catálogo clínico con nombre particularmente extenso',
  description: 'Catálogo reproducible para las pruebas responsive.',
  specialty: 'PSICOLOGIA',
  items: [catalogItem],
};

const pageResponse = <T>(content: T[]) => ({
  content,
  page: {
    number: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
  },
});

async function json(route: Route, body: unknown, status = 200): Promise<void> {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

async function handleApiRoute(route: Route): Promise<void> {
  const request = route.request();
  const { pathname } = new URL(request.url());

  if (pathname.endsWith('/v1/auth/login')) {
    const credentials = request.postDataJSON() as { username?: string } | null;
    if (credentials?.username === 'usuario_invalido') {
      await json(route, { message: 'Credenciales incorrectas' }, 401);
      return;
    }
    await json(route, authResponse);
    return;
  }

  if (pathname.endsWith('/v1/auth/refresh')) {
    await json(route, authResponse);
    return;
  }

  if (pathname.endsWith('/v1/auth/logout')) {
    await json(route, {});
    return;
  }

  if (pathname.endsWith('/v1/settings/clinic')) {
    await json(route, {
      id: 1,
      clinicName: 'Clínica de Especialidades con Nombre Extenso',
      shortName: 'Clínica E2E',
      logoUrl: '',
      contactEmail: 'contacto.extenso@clinica-e2e.example',
      contactPhone: '999999999',
      address: 'Avenida de pruebas responsive 123',
    });
    return;
  }

  if (pathname.includes('/v1/specialties')) {
    await json(route, [
      {
        id: 1,
        code: 'PSICOLOGIA',
        name: 'Psicología clínica con denominación extensa',
        active: true,
        displayOrder: 1,
      },
    ]);
    return;
  }

  if (pathname.endsWith('/v1/users/me')) {
    await json(route, professional);
    return;
  }

  if (pathname.endsWith('/v1/users/professionals')) {
    await json(route, [professional]);
    return;
  }

  if (pathname.endsWith('/v1/users')) {
    await json(route, pageResponse([professional]));
    return;
  }

  if (pathname.endsWith('/v1/dashboard/stats')) {
    await json(route, {
      activePatients: 123456,
      appointmentsToday: 1,
      monthlyIncome: 987654.32,
      upcomingAppointments: [appointment],
      todaysAppointments: [appointment],
      attendanceRate: 95,
      cancelledAppointments: 2,
      newPatientsThisMonth: 123,
      monthlyIncomeGrowth: 12.5,
      activeRiskAlerts: [],
      lowStockSupplies: [supply],
      pendingSoapNotes: [],
      psychometricEvaluationsThisMonth: 25,
      dermatologicalEvaluationsThisMonth: 0,
      dermatologicalProceduresThisMonth: 0,
    });
    return;
  }

  if (pathname.endsWith('/v1/patients/stats')) {
    await json(route, {
      totalPatients: 123456,
      newThisMonth: 123,
      withActiveAlerts: 12,
      minors: 25,
    });
    return;
  }

  if (/\/v1\/patients\/[^/]+$/.test(pathname)) {
    await json(route, patient);
    return;
  }

  if (pathname.includes('/v1/patients')) {
    await json(route, pageResponse([patient]));
    return;
  }

  if (pathname.includes('/v1/appointments')) {
    await json(route, pageResponse([appointment]));
    return;
  }

  if (pathname.endsWith('/v1/schedule-blocks')) {
    await json(route, []);
    return;
  }

  if (pathname.endsWith('/v1/schedules/me')) {
    await json(route, {
      professionalId: professional.id,
      professionalName: `${professional.firstName} ${professional.lastName}`,
      specialty: 'PSICOLOGIA',
      schedules: [],
    });
    return;
  }

  if (pathname.endsWith('/v1/attentions/today-summary')) {
    await json(route, {
      totalToday: 1,
      scheduledToday: 1,
      inProgressToday: 0,
      attendedToday: 0,
      paidToday: 0,
      cancelledToday: 0,
      pendingBillingAmount: 987654.32,
    });
    return;
  }

  if (pathname.endsWith('/v1/attentions')) {
    await json(route, pageResponse([
      {
        id: 1,
        patientId: patient.id,
        patientName: `${patient.firstName} ${patient.lastName}`,
        professionalId: professional.id,
        professionalName: `${professional.firstName} ${professional.lastName}`,
        clinicalServiceId: clinicalService.id,
        clinicalServiceName: clinicalService.name,
        specialty: 'PSICOLOGIA',
        attentionDate: today,
        startTime: '10:00:00',
        status: 'AGENDADA',
        motive: 'Atención de prueba responsive',
      },
    ]));
    return;
  }

  if (pathname.endsWith('/v1/payments/summary')) {
    await json(route, {
      incomeToday: 123456.78,
      incomeMonth: 987654.32,
      monthlyGrowth: 15,
      paymentsCountMonth: 123456,
      averageTicket: 654321.98,
      pendingBalance: 864197.54,
      methodBreakdown: [],
      dailyIncome: [],
      topServices: [],
    });
    return;
  }

  if (pathname.endsWith('/v1/payments')) {
    await json(route, pageResponse([payment]));
    return;
  }

  if (pathname.endsWith('/v1/supplies/stats')) {
    await json(route, {
      totalSupplies: 123456,
      lowStockCount: 12,
      outOfStockCount: 3,
      expiringSoonCount: 7,
      inventoryValue: 987654.32,
    });
    return;
  }

  if (pathname.endsWith('/v1/supplies')) {
    await json(route, pageResponse([supply]));
    return;
  }

  if (pathname.endsWith('/v1/inventory-transactions/recent')) {
    await json(route, []);
    return;
  }

  if (pathname.endsWith('/v1/clinical-services/stats')) {
    await json(route, {
      totalServices: 123456,
      activeCount: 123455,
      averagePrice: 987654.32,
      topByRevenue: [],
      topByQuantity: [],
    });
    return;
  }

  if (pathname.endsWith('/v1/clinical-services/active')) {
    await json(route, [clinicalService]);
    return;
  }

  if (pathname.endsWith('/v1/clinical-services')) {
    await json(route, pageResponse([clinicalService]));
    return;
  }

  if (pathname.endsWith('/v1/tests')) {
    await json(route, pageResponse([
      {
        id: 1,
        name: 'Prueba psicométrica con denominación extensa',
        description: 'Instrumento reproducible para E2E.',
        questions: [],
        active: true,
      },
    ]));
    return;
  }

  if (pathname.endsWith('/v1/catalogs/all')) {
    await json(route, [catalog]);
    return;
  }

  if (pathname.includes('/v1/catalogs/') && pathname.endsWith('/items/active')) {
    await json(route, [catalogItem]);
    return;
  }

  if (pathname.endsWith('/v1/catalogs')) {
    await json(route, pageResponse([catalog]));
    return;
  }

  if (pathname.endsWith('/v1/audit-logs/actions')) {
    await json(route, ['CREATE', 'UPDATE']);
    return;
  }

  if (pathname.endsWith('/v1/audit-logs/entity-types')) {
    await json(route, ['PATIENT', 'APPOINTMENT']);
    return;
  }

  if (pathname.endsWith('/v1/audit-logs')) {
    await json(route, pageResponse([
      {
        id: 1,
        userId: professional.id,
        username: professional.username,
        specialty: 'PSICOLOGIA',
        action: 'UPDATE',
        entityType: 'PATIENT',
        entityId: 'patient-e2e-001',
        detail: 'Detalle de auditoría suficientemente extenso para validar priorización.',
        ip: '127.0.0.1',
        createdAt: `${today}T10:30:00`,
      },
    ]));
    return;
  }

  if (pathname.includes('/v1/public/prescriptions/verify/')) {
    await json(route, {
      valid: true,
      verificationCode: 'DEMO-CODE-123',
      patientName: `${patient.firstName} ${patient.lastName}`,
      patientIdentificationDocument: patient.identificationDocument,
      prescriptionDate: today,
      validUntil: '2027-12-31',
      statusMessage: 'Receta vigente',
      professionalName: `${professional.firstName} ${professional.lastName}`,
      specialty: 'PSICOLOGIA',
      clinicName: 'Clínica E2E',
      items: [],
    });
    return;
  }

  if (request.method() === 'GET') {
    await json(route, pageResponse([]));
    return;
  }

  await json(route, {});
}

export async function installClinicApiMock(page: Page): Promise<void> {
  await page.route('**/api/**', handleApiRoute);
}

export const test = base.extend({
  page: async ({ page }, use) => {
    await installClinicApiMock(page);
    await use(page);
  },
});

export { expect };
