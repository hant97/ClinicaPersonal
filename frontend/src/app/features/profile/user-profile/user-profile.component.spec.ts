import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserProfileComponent } from './user-profile.component';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClinicSettingsService } from '../../../core/services/clinic-settings.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { of } from 'rxjs';

describe('UserProfileComponent', () => {
  let component: UserProfileComponent;
  let fixture: ComponentFixture<UserProfileComponent>;
  let mockUserService: jasmine.SpyObj<UserService>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockClinicSettingsService: jasmine.SpyObj<ClinicSettingsService>;
  let mockToastService: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    mockUserService = jasmine.createSpyObj('UserService', ['getCurrentUserProfile', 'updateProfile', 'updatePassword']);
    mockAuthService = jasmine.createSpyObj('AuthService', ['hasRole', 'getSpecialty', 'loginResponse']);
    mockClinicSettingsService = jasmine.createSpyObj('ClinicSettingsService', ['getSettings', 'updateSettings', 'uploadLogo', 'getLogoUrl']);
    mockToastService = jasmine.createSpyObj('ToastService', ['show']);

    mockUserService.getCurrentUserProfile.and.returnValue(of({
      id: 1,
      username: 'silvi43',
      firstName: 'Silvia',
      lastName: 'Flores',
      email: 'silvi43@hotmail.com',
      phone: '999233111',
      specialty: 'PSICOLOGIA',
      roles: ['ROLE_ADMIN']
    }));

    mockAuthService.hasRole.and.callFake((role: string) => role === 'ROLE_ADMIN');
    mockAuthService.getSpecialty.and.returnValue('PSICOLOGIA');
    mockClinicSettingsService.getSettings.and.returnValue(of({
      clinicName: 'Clínica Integral',
      shortName: 'Clínica',
      logoUrl: '',
      contactEmail: 'contacto@clinica.com',
      contactPhone: '999888777',
      address: 'Av. Siempre Viva 123'
    }));

    await TestBed.configureTestingModule({
      imports: [UserProfileComponent],
      providers: [
        { provide: UserService, useValue: mockUserService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: ClinicSettingsService, useValue: mockClinicSettingsService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the profile component', () => {
    expect(component).toBeTruthy();
  });

  it('should load current user profile on init', () => {
    expect(mockUserService.getCurrentUserProfile).toHaveBeenCalled();
    expect(component.profileForm.get('firstName')?.value).toBe('Silvia');
    expect(component.profileForm.get('lastName')?.value).toBe('Flores');
    expect(component.userFullName).toBe('Silvia Flores');
    expect(component.avatarLetter).toBe('S');
  });

  it('should switch tabs properly', () => {
    expect(component.activeTab).toBe('personal');
    component.switchTab('password');
    expect(component.activeTab).toBe('password');
    component.switchTab('clinic');
    expect(component.activeTab).toBe('clinic');
  });

  it('should validate password requirements correctly', () => {
    expect(component.hasMinLength('12345678')).toBeTrue();
    expect(component.hasMinLength('1234')).toBeFalse();
    expect(component.hasUppercase('Password1!')).toBeTrue();
    expect(component.hasLowercase('Password1!')).toBeTrue();
    expect(component.hasNumber('Password1!')).toBeTrue();
    expect(component.hasSpecialChar('Password1!')).toBeTrue();
    expect(component.hasSpecialChar('Password123')).toBeFalse();
  });

  it('should save profile when valid', () => {
    mockUserService.updateProfile.and.returnValue(of({
      id: 1,
      username: 'silvi43',
      firstName: 'Silvia Maria',
      lastName: 'Flores',
      email: 'silvi43@hotmail.com',
      phone: '999233111'
    }));

    component.profileForm.patchValue({
      firstName: 'Silvia Maria',
      lastName: 'Flores',
      email: 'silvi43@hotmail.com',
      phone: '999233111'
    });

    component.saveProfile();
    expect(mockUserService.updateProfile).toHaveBeenCalled();
    expect(mockToastService.show).toHaveBeenCalledWith('Perfil actualizado correctamente', 'success');
  });
});
