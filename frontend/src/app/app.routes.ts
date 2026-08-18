import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard.component';
import { PatientListComponent } from './features/patients/patient-list/patient-list.component';
import { PatientFormComponent } from './features/patients/patient-form/patient-form.component';
import { PatientDetailComponent } from './features/patients/patient-detail/patient-detail.component';
import { AgendaComponent } from './features/agenda/agenda/agenda.component';
import { BillingComponent } from './features/billing/billing/billing.component';
import { authGuard } from './core/guards/auth.guard';
import { specialtyGuard } from './core/guards/specialty.guard';
import { adminGuard } from './core/guards/admin.guard';
import { siteAdminGuard } from './core/guards/site-admin.guard';

import { TestsCatalogListComponent } from './features/tests-catalog/tests-catalog-list/tests-catalog-list.component';
import { TestsCatalogFormComponent } from './features/tests-catalog/tests-catalog-form/tests-catalog-form.component';
import { InventoryListComponent } from './features/inventory/inventory-list/inventory-list.component';
import { ClinicalServicesListComponent } from './features/clinical-services/clinical-services-list/clinical-services-list.component';
import { CatalogManagementComponent } from './features/settings/catalog-management/catalog-management.component';
import { UserManagementComponent } from './features/settings/user-management/user-management.component';
import { UserProfileComponent } from './features/profile/user-profile/user-profile.component';
import { LandingComponent } from './features/public/landing/landing.component';
import { PrescriptionVerifyComponent } from './features/public/prescription-verify/prescription-verify.component';
import { NotFoundComponent } from './features/public/not-found/not-found.component';

import { ClinicalSessionPageComponent } from './features/patients/clinical-session-page/clinical-session-page.component';

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full', title: 'Dermatología y Psicología' },
  { path: 'login', component: LoginComponent, title: 'Iniciar sesión' },
  { path: 'verificar-receta/:code', component: PrescriptionVerifyComponent, title: 'Verificación de Receta Médica' },
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
      { path: 'dashboard', component: DashboardComponent, title: 'Dashboard' },
      { path: 'patients', component: PatientListComponent, title: 'Pacientes' },
      { path: 'patients/new', component: PatientFormComponent, title: 'Nuevo Paciente' },
      { path: 'patients/:identifier/edit', component: PatientFormComponent, title: 'Editar Paciente' },
      { path: 'patients/:id', component: PatientDetailComponent, title: 'Ficha del paciente' },
      { path: 'patients/:id/sessions/new', component: ClinicalSessionPageComponent, title: 'Nueva Consulta Clínica' },
      { path: 'patients/:id/sessions/:sessionId/edit', component: ClinicalSessionPageComponent, title: 'Editar Consulta Clínica' },
      { path: 'agenda', component: AgendaComponent, title: 'Agenda' },
      { path: 'billing', component: BillingComponent, title: 'Cobros' },

      // Rutas exclusivas de Psicología
      { path: 'tests-catalog', component: TestsCatalogListComponent, canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Pruebas psicométricas' },
      { path: 'tests-catalog/new', component: TestsCatalogFormComponent, canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Nueva prueba' },
      { path: 'tests-catalog/edit/:id', component: TestsCatalogFormComponent, canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Editar prueba' },

      { path: 'inventory', component: InventoryListComponent, title: 'Inventario' },
      { path: 'services', component: ClinicalServicesListComponent, title: 'Servicios clínicos' },
      { path: 'settings/catalogs', component: CatalogManagementComponent, title: 'Catálogos' },
      { path: 'settings/users', component: UserManagementComponent, canActivate: [adminGuard], title: 'Gestión de Personal y Cuentas' },
      { path: 'settings/website', redirectTo: '/editar-sitio', pathMatch: 'full' },
      { path: 'profile', component: UserProfileComponent, title: 'Mi perfil' }
    ]
  },
  { path: '**', component: NotFoundComponent, title: 'Página no encontrada' }
];
