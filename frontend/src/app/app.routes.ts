import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { specialtyGuard } from './core/guards/specialty.guard';
import { adminGuard } from './core/guards/admin.guard';
import { siteAdminGuard } from './core/guards/site-admin.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/public/landing/landing.component').then(m => m.LandingComponent),
    title: 'Dermatología y Psicología'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    title: 'Iniciar sesión'
  },
  {
    path: 'verificar-receta/:code',
    loadComponent: () => import('./features/public/prescription-verify/prescription-verify.component').then(m => m.PrescriptionVerifyComponent),
    title: 'Verificación de Receta Médica'
  },
  {
    path: 'confirmar-cita/:token',
    loadComponent: () => import('./features/public/appointment-confirm/appointment-confirm.component').then(m => m.AppointmentConfirmComponent),
    title: 'Confirmación de Cita'
  },
  {
    path: 'editar-sitio',
    canActivate: [siteAdminGuard],
    loadComponent: () => import('./features/settings/landing-editor/landing-editor.component').then(m => m.LandingEditorComponent),
    title: 'Editar sitio'
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard/dashboard.component').then(m => m.DashboardComponent), title: 'Dashboard' },
      { path: 'patients', loadComponent: () => import('./features/patients/patient-list/patient-list.component').then(m => m.PatientListComponent), title: 'Pacientes' },
      { path: 'patients/new', loadComponent: () => import('./features/patients/patient-form/patient-form.component').then(m => m.PatientFormComponent), title: 'Nuevo Paciente' },
      { path: 'patients/:identifier/edit', loadComponent: () => import('./features/patients/patient-form/patient-form.component').then(m => m.PatientFormComponent), title: 'Editar Paciente' },
      { path: 'patients/:id', loadComponent: () => import('./features/patients/patient-detail/patient-detail.component').then(m => m.PatientDetailComponent), title: 'Ficha del paciente' },
      { path: 'patients/:id/sessions/new', loadComponent: () => import('./features/patients/clinical-session-page/clinical-session-page.component').then(m => m.ClinicalSessionPageComponent), title: 'Nueva Consulta Clínica' },
      { path: 'patients/:id/sessions/:sessionId/edit', loadComponent: () => import('./features/patients/clinical-session-page/clinical-session-page.component').then(m => m.ClinicalSessionPageComponent), title: 'Editar Consulta Clínica' },
      { path: 'agenda', loadComponent: () => import('./features/agenda/agenda/agenda.component').then(m => m.AgendaComponent), title: 'Agenda' },
      { path: 'attentions', loadComponent: () => import('./features/attentions/attention-list/attention-list.component').then(m => m.AttentionListComponent), title: 'Atenciones' },
      { path: 'billing', loadComponent: () => import('./features/billing/billing/billing.component').then(m => m.BillingComponent), title: 'Cobros' },

      // Rutas exclusivas de Psicología
      { path: 'tests-catalog', loadComponent: () => import('./features/tests-catalog/tests-catalog-list/tests-catalog-list.component').then(m => m.TestsCatalogListComponent), canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Pruebas psicométricas' },
      { path: 'tests-catalog/new', loadComponent: () => import('./features/tests-catalog/tests-catalog-form/tests-catalog-form.component').then(m => m.TestsCatalogFormComponent), canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Nueva prueba' },
      { path: 'tests-catalog/edit/:id', loadComponent: () => import('./features/tests-catalog/tests-catalog-form/tests-catalog-form.component').then(m => m.TestsCatalogFormComponent), canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Editar prueba' },

      { path: 'inventory', loadComponent: () => import('./features/inventory/inventory-list/inventory-list.component').then(m => m.InventoryListComponent), title: 'Inventario' },
      { path: 'services', loadComponent: () => import('./features/clinical-services/clinical-services-list/clinical-services-list.component').then(m => m.ClinicalServicesListComponent), title: 'Servicios clínicos' },
      { path: 'settings/catalogs', loadComponent: () => import('./features/settings/catalog-management/catalog-management.component').then(m => m.CatalogManagementComponent), canActivate: [adminGuard], title: 'Catálogos' },
      { path: 'settings/users', loadComponent: () => import('./features/settings/user-management/user-management.component').then(m => m.UserManagementComponent), canActivate: [adminGuard], title: 'Gestión de Personal y Cuentas' },
      { path: 'settings/audit', loadComponent: () => import('./features/settings/audit-log/audit-log.component').then(m => m.AuditLogComponent), canActivate: [adminGuard], title: 'Registro de Auditoría' },
      { path: 'settings/website', redirectTo: '/editar-sitio', pathMatch: 'full' },
      { path: 'profile', loadComponent: () => import('./features/profile/user-profile/user-profile.component').then(m => m.UserProfileComponent), title: 'Mi perfil' }
    ]
  },
  { path: '**', loadComponent: () => import('./features/public/not-found/not-found.component').then(m => m.NotFoundComponent), title: 'Página no encontrada' }
];
