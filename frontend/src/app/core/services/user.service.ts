import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserProfile, UpdateProfileRequest, UpdatePasswordRequest } from '../models/user-profile.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/v1/users`;

  constructor(private http: HttpClient) { }

  getCurrentUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/me`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/me`, request);
  }

  updatePassword(request: UpdatePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/password`, request);
  }
}
