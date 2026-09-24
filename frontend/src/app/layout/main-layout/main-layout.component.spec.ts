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
    localStorage.clear();
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

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should open and close the mobile navigation', () => {
    component.toggleSidebar();
    expect(component.isSidebarOpen).toBe(true);

    component.closeSidebar();
    expect(component.isSidebarOpen).toBe(false);
  });

  it('should toggle sidebar collapse state', () => {
    component.isDesktopSidebar = true;
    const initialState = component.isSidebarExpanded;
    component.toggleSidebarCollapse();
    expect(component.isSidebarExpanded).toBe(!initialState);
    expect(localStorage.getItem('sidebar_expanded')).toBe(JSON.stringify(!initialState));

    component.toggleSidebarCollapse();
    expect(component.isSidebarExpanded).toBe(initialState);
  });

  it('should keep the sidebar compact outside wide desktop', () => {
    component.isDesktopSidebar = false;
    component.isSidebarExpanded = false;

    component.toggleSidebarCollapse();

    expect(component.isSidebarExpanded).toBe(false);
    expect(localStorage.getItem('sidebar_expanded')).toBeNull();
  });

  it('should declare compact tablet and expandable desktop classes', () => {
    const sidebar = fixture.nativeElement.querySelector('#main-navigation') as HTMLElement;
    const collapseControl = fixture.nativeElement.querySelector('[data-testid="sidebar-collapse"]') as HTMLElement;

    expect(sidebar.classList.contains('md:w-16')).toBe(true);
    expect(sidebar.classList.contains('lg:w-64')).toBe(false);
    expect(collapseControl.classList.contains('xl:flex')).toBe(true);
  });

  it('should toggle and close the profile dropdown menu', () => {
    expect(component.isProfileMenuOpen).toBe(false);

    component.toggleProfileMenu();
    expect(component.isProfileMenuOpen).toBe(true);

    component.closeProfileMenu();
    expect(component.isProfileMenuOpen).toBe(false);
  });

  it('should close profile menu on escape key', () => {
    const profileToggle = fixture.nativeElement.querySelector('[aria-haspopup="menu"]') as HTMLButtonElement;
    expect(profileToggle).not.toBeNull();
    const focusSpy = vi.spyOn(profileToggle, 'focus');
    component.isProfileMenuOpen = true;
    component.onEscape();
    expect(component.isProfileMenuOpen).toBe(false);
    expect(focusSpy).toHaveBeenCalled();
  });

  it('should call authService.logout and close menu on logout', () => {
    const authService = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    const logoutSpy = vi.spyOn(authService, 'logout');
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.isProfileMenuOpen = true;

    component.logout();

    expect(logoutSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledTimes(1);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
    expect(component.isProfileMenuOpen).toBe(false);
  });
});
