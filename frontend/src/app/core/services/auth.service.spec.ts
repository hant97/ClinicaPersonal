import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { clinicalDraftKey } from '../utils/clinical-draft.util';

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
      expect(service.isLoggedIn()).toBe(true);
      expect(service.hasRole('ROLE_ADMIN')).toBe(true);
      expect(service.hasRole('ROLE_SITE_ADMIN')).toBe(false);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockResponse);
  });

  it('stores the landing administrator role returned by the API', () => {
    service.login({ username: 'admin', password: 'Admin!1234' }).subscribe(() => {
      expect(service.hasRole('ROLE_SITE_ADMIN')).toBe(true);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    req.flush({ token: 'site-admin-token', specialty: 'PSICOLOGIA', roles: ['ROLE_SITE_ADMIN'] });
  });

  it('should clear token on logout', () => {
    service.login({ username: 'admin', password: 'Password1!' }).subscribe();
    const loginRequest = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    loginRequest.flush({ token: 'sample-token' });
    expect(service.isLoggedIn()).toBe(true);

    service.logout();
    const logoutRequest = httpMock.expectOne(`${environment.apiUrl}/v1/auth/logout`);
    logoutRequest.flush(null, { status: 204, statusText: 'No Content' });
    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
  });

  it('should read the username from the token subject', () => {
    const payload = btoa(JSON.stringify({ sub: 'dra.perez' })).replace(/=+$/, '');
    service.loginResponse({ token: `header.${payload}.signature` });

    expect(service.getUsername()).toBe('dra.perez');
  });

  it('should return null username when there is no valid token', () => {
    expect(service.getUsername()).toBeNull();
    service.loginResponse({ token: 'not-a-jwt' });
    expect(service.getUsername()).toBeNull();
  });

  it('should remove clinical drafts on explicit logout but keep them when the session expires', () => {
    localStorage.setItem(clinicalDraftKey('admin', 1), '{"subjective":"nota"}');

    service.clearSession();
    expect(localStorage.getItem(clinicalDraftKey('admin', 1))).not.toBeNull();

    service.logout();
    expect(localStorage.getItem(clinicalDraftKey('admin', 1))).toBeNull();
  });
});
