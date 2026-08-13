import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { MainLayoutComponent } from './main-layout.component';
import { ClinicSettingsService } from '../../core/services/clinic-settings.service';
import { UserService } from '../../core/services/user.service';
import { SpecialtyService } from '../../core/services/specialty.service';
import { AuthService } from '../../core/services/auth.service';

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
        }
      ]
    })
    .compileComponents();

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
});
