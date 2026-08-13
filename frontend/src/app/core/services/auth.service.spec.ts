import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should authenticate user and keep the access token in memory', () => {
    const mockResponse = { token: 'mock-jwt-token', specialty: 'PSICOLOGIA', roles: ['ROLE_ADMIN'] };

    service.login({ username: 'admin', password: 'password123' }).subscribe(res => {
      expect(res.token).toBe('mock-jwt-token');
      expect(service.getToken()).toBe('mock-jwt-token');
      expect(service.isLoggedIn()).toBeTrue();
      expect(service.hasRole('ROLE_ADMIN')).toBeTrue();
      expect(service.hasRole('ROLE_SITE_ADMIN')).toBeFalse();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockResponse);
  });

  it('stores the landing administrator role returned by the API', () => {
    service.login({ username: 'admin', password: 'Admin!1234' }).subscribe(() => {
      expect(service.hasRole('ROLE_SITE_ADMIN')).toBeTrue();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    req.flush({ token: 'site-admin-token', specialty: 'PSICOLOGIA', roles: ['ROLE_SITE_ADMIN'] });
  });

  it('should clear token on logout', () => {
    service.login({ username: 'admin', password: 'Password1!' }).subscribe();
    const loginRequest = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    loginRequest.flush({ token: 'sample-token' });
    expect(service.isLoggedIn()).toBeTrue();

    service.logout();
    const logoutRequest = httpMock.expectOne(`${environment.apiUrl}/v1/auth/logout`);
    logoutRequest.flush(null, { status: 204, statusText: 'No Content' });
    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
  });
});
