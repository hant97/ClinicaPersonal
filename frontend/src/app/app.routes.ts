import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard.component';
import { PatientListComponent } from './features/patients/patient-list/patient-list.component';
import { PatientDetailComponent } from './features/patients/patient-detail/patient-detail.component';
import { DermatologicalEvaluationPageComponent } from './features/patients/dermatological-evaluation-page/dermatological-evaluation-page.component';
import { AgendaComponent } from './features/agenda/agenda/agenda.component';
import { BillingComponent } from './features/billing/billing/billing.component';
import { authGuard } from './core/guards/auth.guard';
import { specialtyGuard } from './core/guards/specialty.guard';

import { TestsCatalogListComponent } from './features/tests-catalog/tests-catalog-list/tests-catalog-list.component';
import { TestsCatalogFormComponent } from './features/tests-catalog/tests-catalog-form/tests-catalog-form.component';
import { InventoryListComponent } from './features/inventory/inventory-list/inventory-list.component';
import { ClinicalServicesListComponent } from './features/clinical-services/clinical-services-list/clinical-services-list.component';
import { CatalogManagementComponent } from './features/settings/catalog-management/catalog-management.component';
import { UserProfileComponent } from './features/profile/user-profile/user-profile.component';
import { LandingComponent } from './features/public/landing/landing.component';
import { NotFoundComponent } from './features/public/not-found/not-found.component';
import { WebsiteSettingsComponent } from './features/settings/website-settings/website-settings.component';
import { siteAdminGuard } from './core/guards/site-admin.guard';

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full', title: 'Dermatología y Psicología' },
  { path: 'login', component: LoginComponent, title: 'Iniciar sesión' },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, title: 'Dashboard' },
      { path: 'patients', component: PatientListComponent, title: 'Pacientes' },
      { path: 'patients/:id', component: PatientDetailComponent, title: 'Ficha del paciente' },
      {
        path: 'patients/:patientId/dermatological-evaluations',
        component: DermatologicalEvaluationPageComponent,
        canActivate: [specialtyGuard('DERMATOLOGIA')],
        title: 'Evaluaciones dermatológicas'
      },
      { path: 'agenda', component: AgendaComponent, title: 'Agenda' },
      { path: 'billing', component: BillingComponent, title: 'Cobros' },

      // Rutas exclusivas de Psicología
      { path: 'tests-catalog', component: TestsCatalogListComponent, canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Pruebas psicométricas' },
      { path: 'tests-catalog/new', component: TestsCatalogFormComponent, canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Nueva prueba' },
      { path: 'tests-catalog/edit/:id', component: TestsCatalogFormComponent, canActivate: [specialtyGuard('PSICOLOGIA')], title: 'Editar prueba' },

      { path: 'inventory', component: InventoryListComponent, title: 'Inventario' },
      { path: 'services', component: ClinicalServicesListComponent, title: 'Servicios clínicos' },
      { path: 'settings/catalogs', component: CatalogManagementComponent, title: 'Catálogos' },
      { path: 'settings/website', component: WebsiteSettingsComponent, canActivate: [siteAdminGuard], title: 'Configuración del sitio web' },
      { path: 'profile', component: UserProfileComponent, title: 'Mi perfil' }
    ]
  },
  { path: '**', component: NotFoundComponent, title: 'Página no encontrada' }
];
