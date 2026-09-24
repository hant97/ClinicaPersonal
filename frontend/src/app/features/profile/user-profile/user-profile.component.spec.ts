import type { MockedObject } from 'vitest';
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
  let mockUserService: MockedObject<UserService>;
  let mockAuthService: MockedObject<AuthService>;
  let mockClinicSettingsService: MockedObject<ClinicSettingsService>;
  let mockToastService: MockedObject<ToastService>;

  beforeEach(async () => {
    mockUserService = {
      getCurrentUserProfile: vi.fn().mockName('UserService.getCurrentUserProfile'),
      updateProfile: vi.fn().mockName('UserService.updateProfile'),
      updatePassword: vi.fn().mockName('UserService.updatePassword')
    } as unknown as MockedObject<UserService>;
    mockAuthService = {
      hasRole: vi.fn().mockName('AuthService.hasRole'),
      getSpecialty: vi.fn().mockName('AuthService.getSpecialty'),
      loginResponse: vi.fn().mockName('AuthService.loginResponse')
    } as unknown as MockedObject<AuthService>;
    mockClinicSettingsService = {
      getSettings: vi.fn().mockName('ClinicSettingsService.getSettings'),
      updateSettings: vi.fn().mockName('ClinicSettingsService.updateSettings'),
      uploadLogo: vi.fn().mockName('ClinicSettingsService.uploadLogo'),
      getLogoUrl: vi.fn().mockName('ClinicSettingsService.getLogoUrl')
    } as unknown as MockedObject<ClinicSettingsService>;
    mockToastService = {
      show: vi.fn().mockName('ToastService.show')
    } as unknown as MockedObject<ToastService>;

    mockUserService.getCurrentUserProfile.mockReturnValue(of({
      id: 1,
      username: 'silvi43',
      firstName: 'Silvia',
      lastName: 'Flores',
      email: 'silvi43@hotmail.com',
      phone: '999233111',
      specialty: 'PSICOLOGIA',
      roles: ['ROLE_ADMIN']
    }));

    mockAuthService.hasRole.mockImplementation((role: string) => role === 'ROLE_ADMIN');
    mockAuthService.getSpecialty.mockReturnValue('PSICOLOGIA');
    mockClinicSettingsService.getSettings.mockReturnValue(of({
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
    expect(component.hasMinLength('12345678')).toBe(true);
    expect(component.hasMinLength('1234')).toBe(false);
    expect(component.hasUppercase('Password1!')).toBe(true);
    expect(component.hasLowercase('Password1!')).toBe(true);
    expect(component.hasNumber('Password1!')).toBe(true);
    expect(component.hasSpecialChar('Password1!')).toBe(true);
    expect(component.hasSpecialChar('Password123')).toBe(false);
  });

  it('should save profile when valid', () => {
    mockUserService.updateProfile.mockReturnValue(of({
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
