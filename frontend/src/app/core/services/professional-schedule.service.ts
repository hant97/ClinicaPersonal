import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WeeklySchedule } from '../models/professional-schedule.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProfessionalScheduleService {
  private apiUrl = `${environment.apiUrl}/v1/schedules`;

  constructor(private http: HttpClient) {}

  getWeeklySchedule(professionalId: number): Observable<WeeklySchedule> {
    return this.http.get<WeeklySchedule>(`${this.apiUrl}/professional/${professionalId}`);
  }

  saveWeeklySchedule(weeklySchedule: WeeklySchedule): Observable<WeeklySchedule> {
    return this.http.put<WeeklySchedule>(`${this.apiUrl}/professional/${weeklySchedule.professionalId}`, weeklySchedule);
  }

  getMyWeeklySchedule(): Observable<WeeklySchedule> {
    return this.http.get<WeeklySchedule>(`${this.apiUrl}/me`);
  }

  saveMyWeeklySchedule(weeklySchedule: WeeklySchedule): Observable<WeeklySchedule> {
    return this.http.put<WeeklySchedule>(`${this.apiUrl}/me`, weeklySchedule);
  }
}
