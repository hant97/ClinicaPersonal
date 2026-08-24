import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { provideRouter, Router } from '@angular/router';

import { MainLayoutComponent } from './main-layout.component';
import { ClinicSettingsService } from '../../core/services/clinic-settings.service';
import { UserService } from '../../core/services/user.service';
import { SpecialtyService } from '../../core/services/specialty.service';
import { AuthService } from '../../core/services/auth.service';
import { PatientService } from '../../core/services/patient/patient.service';

describe('MainLayoutComponent', () => {
  let component: MainLayoutComponent;
  let fixture: ComponentFixture<MainLayoutComponent>;
  const profileUpdated$ = new BehaviorSubject({ firstName: 'Ana', username: 'ana' });
  const settings$ = new BehaviorSubject({
    clinicName: 'Clínica Central', shortName: 'Central', logoUrl: '', contactEmail: '', contactPhone: '', address: ''
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MainLayoutComponent],
      providers: [
        provideRouter([]),
        {
          provide: UserService,
          useValue: {
            profileUpdated$: profileUpdated$.asObservable(),
            getCurrentUserProfile: () => of({ firstName: 'Ana', username: 'ana' })
          }
        },
        {
          provide: ClinicSettingsService,
          useValue: {
            settings$: settings$.asObservable(),
            getLogoUrl: (path: string) => path
          }
        },
        {
          provide: SpecialtyService,
          useValue: { isPsychology: () => false, isDermatology: () => true }
        },
        {
          provide: AuthService,
          useValue: { logout: () => undefined, hasRole: () => false }
        },
        {
          provide: PatientService,
          useValue: { search: () => of({ content: [], page: { totalPages: 0, totalElements: 0 } }) }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MainLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should open and close the mobile navigation', () => {
    component.toggleSidebar();
    expect(component.isSidebarOpen).toBeTrue();

    component.closeSidebar();
    expect(component.isSidebarOpen).toBeFalse();
  });

  it('should toggle sidebar collapse state', () => {
    const initialState = component.isSidebarExpanded;
    component.toggleSidebarCollapse();
    expect(component.isSidebarExpanded).toBe(!initialState);

    component.toggleSidebarCollapse();
    expect(component.isSidebarExpanded).toBe(initialState);
  });

  it('should toggle and close the profile dropdown menu', () => {
    expect(component.isProfileMenuOpen).toBeFalse();

    component.toggleProfileMenu();
    expect(component.isProfileMenuOpen).toBeTrue();

    component.closeProfileMenu();
    expect(component.isProfileMenuOpen).toBeFalse();
  });

  it('should close profile menu on escape key', () => {
    component.isProfileMenuOpen = true;
    component.onEscape();
    expect(component.isProfileMenuOpen).toBeFalse();
  });

  it('should call authService.logout and close menu on logout', () => {
    const authService = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    const logoutSpy = spyOn(authService, 'logout');
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);
    component.isProfileMenuOpen = true;

    component.logout();

    expect(logoutSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledOnceWith(['/login']);
    expect(component.isProfileMenuOpen).toBeFalse();
  });
});
