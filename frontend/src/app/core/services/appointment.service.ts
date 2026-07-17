import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment } from '../models/appointment.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private apiUrl = `${environment.apiUrl}/v1/appointments`;

  constructor(private http: HttpClient) { }

  getAll(page: number = 0, size: number = 1000): Observable<PageResponse<Appointment>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Appointment>>(this.apiUrl, { params });
  }

  search(searchTerm?: string, status?: string, startDate?: string, endDate?: string, page: number = 0, size: number = 1000): Observable<PageResponse<Appointment>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (searchTerm) params = params.set('searchTerm', searchTerm);
    if (status && status !== 'ALL') params = params.set('status', status);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<PageResponse<Appointment>>(`${this.apiUrl}/search`, { params });
  }

  getByPatientId(patientId: number, page: number = 0, size: number = 1000): Observable<PageResponse<Appointment>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Appointment>>(`${this.apiUrl}/patient/${patientId}`, { params });
  }

  create(appointment: Appointment): Observable<Appointment> {
    return this.http.post<Appointment>(this.apiUrl, appointment);
  }

  update(id: number, appointment: Appointment): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.apiUrl}/${id}`, appointment);
  }

  updateStatus(id: number, status: string): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.apiUrl}/${id}/status`, { status });
  }
}
