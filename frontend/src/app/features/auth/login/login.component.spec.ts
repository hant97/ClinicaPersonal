import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();

    const authServiceSpy = jasmine.createSpyObj('AuthService', ['hasRole', 'login']);
    authServiceSpy.hasRole.and.returnValue(false);
    authServiceSpy.login.and.returnValue(of({ token: 'mock-token' } as any));

    await TestBed.configureTestingModule({
      imports: [LoginComponent, HttpClientTestingModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl');
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.showPassword).toBeFalse();
    expect(component.loginForm.get('rememberUsername')).toBeTruthy();
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword).toBeFalse();

    component.togglePasswordVisibility();
    expect(component.showPassword).toBeTrue();

    const passwordInput = fixture.nativeElement.querySelector('#password');
    fixture.detectChanges();
    expect(passwordInput.type).toBe('text');

    component.togglePasswordVisibility();
    expect(component.showPassword).toBeFalse();
    fixture.detectChanges();
    expect(passwordInput.type).toBe('password');
  });

  it('should load remembered username from localStorage on ngOnInit', () => {
    localStorage.setItem(LoginComponent.REMEMBERED_USERNAME_KEY, 'doctor.test');
    
    const newFixture = TestBed.createComponent(LoginComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.loginForm.get('username')?.value).toBe('doctor.test');
    expect(newComponent.loginForm.get('rememberUsername')?.value).toBeTrue();
  });

  it('should save username in localStorage when rememberUsername is true on successful login', () => {
    component.loginForm.setValue({
      username: 'doctor.ana',
      password: 'Password123!',
      rememberUsername: true
    });

    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      username: 'doctor.ana',
      password: 'Password123!'
    });
    expect(localStorage.getItem(LoginComponent.REMEMBERED_USERNAME_KEY)).toBe('doctor.ana');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should remove username from localStorage when rememberUsername is false on successful login', () => {
    localStorage.setItem(LoginComponent.REMEMBERED_USERNAME_KEY, 'doctor.previous');

    component.loginForm.setValue({
      username: 'doctor.ana',
      password: 'Password123!',
      rememberUsername: false
    });

    component.onSubmit();

    expect(localStorage.getItem(LoginComponent.REMEMBERED_USERNAME_KEY)).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });
});

