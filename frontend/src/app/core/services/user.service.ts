import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  UserProfile,
  CreateUserRequest,
  AdminUpdateUserRequest,
  AdminResetPasswordRequest,
  UpdateProfileRequest,
  UpdatePasswordRequest
} from '../models/user-profile.model';
import { PageResponse } from '../models/page.model';
import { AuthResponse } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/v1/users`;
  private profileUpdatedSubject = new Subject<UserProfile>();
  profileUpdated$ = this.profileUpdatedSubject.asObservable();

  constructor(private http: HttpClient) { }

  getCurrentUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/me`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/me`, request).pipe(
      tap(profile => this.profileUpdatedSubject.next(profile))
    );
  }

  updatePassword(request: UpdatePasswordRequest): Observable<AuthResponse> {
    return this.http.put<AuthResponse>(`${this.apiUrl}/me/password`, request);
  }

  getAllUsers(
    query?: string,
    specialty?: string,
    enabled?: boolean,
    page = 0,
    size = 10
  ): Observable<PageResponse<UserProfile>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (query && query.trim()) {
      params = params.set('query', query.trim());
    }
    if (specialty && specialty.trim()) {
      params = params.set('specialty', specialty.trim());
    }
    if (enabled !== undefined && enabled !== null) {
      params = params.set('enabled', enabled.toString());
    }

    return this.http.get<PageResponse<UserProfile>>(this.apiUrl, { params });
  }

  getUserById(id: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${id}`);
  }

  createUser(request: CreateUserRequest): Observable<UserProfile> {
    return this.http.post<UserProfile>(this.apiUrl, request);
  }

  adminUpdateUser(id: number, request: AdminUpdateUserRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/${id}`, request);
  }

  toggleUserStatus(id: number, enabled: boolean): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/status`, null, {
      params: new HttpParams().set('enabled', enabled.toString())
    });
  }

  adminResetPassword(id: number, request: AdminResetPasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/reset-password`, request);
  }

  getProfessionals(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.apiUrl}/professionals`);
  }
}
