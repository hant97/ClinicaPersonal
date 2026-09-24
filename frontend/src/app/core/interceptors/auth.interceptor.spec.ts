import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    authService.loginResponse({ token: 'token-de-sesion' });
  });

  afterEach(() => httpMock.verify());

  it('adds the bearer token to API requests', () => {
    http.get(`${environment.apiUrl}/v1/patients`).subscribe();

    const request = httpMock.expectOne(`${environment.apiUrl}/v1/patients`);
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-de-sesion');
    request.flush({});
  });

  it('never sends the token to other domains', () => {
    http.get('https://example.org/imagen.png').subscribe();

    const request = httpMock.expectOne('https://example.org/imagen.png');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('requires a path boundary after the API base URL', () => {
    http.get(`${environment.apiUrl}.example.org/v1/patients`).subscribe();

    const request = httpMock.expectOne(`${environment.apiUrl}.example.org/v1/patients`);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});
