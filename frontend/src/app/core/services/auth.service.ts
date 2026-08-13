import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, catchError, finalize, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  specialty?: string;
  roles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/v1/auth`;
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  private accessToken: string | null = null;
  private specialty: string | null = null;
  private roles: string[] = [];
  private refreshInFlight$: Observable<AuthResponse> | null = null;

  constructor(private http: HttpClient) { }

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials, { withCredentials: true }).pipe(
      tap(response => {
        if (response && response.token) {
          this.storeResponse(response);
          this.isAuthenticatedSubject.next(true);
        }
      })
    );
  }

  logout(): void {
    if (this.accessToken) {
      this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).subscribe({ complete: () => this.clearSession(), error: () => this.clearSession() });
    } else {
      this.clearSession();
    }
  }

  refresh(): Observable<AuthResponse> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, { withCredentials: true }).pipe(
        tap(response => this.storeResponse(response)),
        finalize(() => this.refreshInFlight$ = null),
        shareReplay(1)
      );
    }
    return this.refreshInFlight$;
  }

  clearSession(): void {
    this.accessToken = null;
    this.specialty = null;
    this.roles = [];
    this.isAuthenticatedSubject.next(false);
  }

  getToken(): string | null {
    return this.accessToken;
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  private hasToken(): boolean {
    return !!this.accessToken;
  }

  getSpecialty(): string | null { return this.specialty; }
  hasRole(role: string): boolean { return this.roles.includes(role); }
  loginResponse(response: AuthResponse): void { this.storeResponse(response); }

  private storeResponse(response: AuthResponse): void {
    this.accessToken = response.token;
    this.specialty = response.specialty ?? null;
    this.roles = response.roles ?? [];
    this.isAuthenticatedSubject.next(true);
  }
}
