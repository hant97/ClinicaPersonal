import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard.component';
import { PatientListComponent } from './features/patients/patient-list/patient-list.component';
import { PatientDetailComponent } from './features/patients/patient-detail/patient-detail.component';
import { AgendaComponent } from './features/agenda/agenda/agenda.component';
import { BillingComponent } from './features/billing/billing/billing.component';
import { authGuard } from './core/guards/auth.guard';

import { TestsCatalogListComponent } from './features/tests-catalog/tests-catalog-list/tests-catalog-list.component';
import { TestsCatalogFormComponent } from './features/tests-catalog/tests-catalog-form/tests-catalog-form.component';
import { InventoryListComponent } from './features/inventory/inventory-list/inventory-list.component';
import { ClinicalServicesListComponent } from './features/clinical-services/clinical-services-list/clinical-services-list.component';
import { CatalogManagementComponent } from './features/settings/catalog-management/catalog-management.component';
import { UserProfileComponent } from './features/profile/user-profile/user-profile.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { 
    path: '', 
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'patients', component: PatientListComponent },
      { path: 'patients/:id', component: PatientDetailComponent },
      { path: 'agenda', component: AgendaComponent },
      { path: 'billing', component: BillingComponent },
      { path: 'tests-catalog', component: TestsCatalogListComponent },
      { path: 'tests-catalog/new', component: TestsCatalogFormComponent },
      { path: 'tests-catalog/edit/:id', component: TestsCatalogFormComponent },
      { path: 'inventory', component: InventoryListComponent },
      { path: 'services', component: ClinicalServicesListComponent },
      { path: 'settings/catalogs', component: CatalogManagementComponent },
      { path: 'profile', component: UserProfileComponent }
    ]
  }
];
