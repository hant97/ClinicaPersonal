import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment, PublicAppointmentConfirmation } from '../models/appointment.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private apiUrl = `${environment.apiUrl}/v1/appointments`;

  constructor(private http: HttpClient) { }

  getAll(page: number = 0, size: number = 100): Observable<PageResponse<Appointment>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Appointment>>(this.apiUrl, { params });
  }

  search(
    searchTerm?: string,
    status?: string,
    startDate?: string,
    endDate?: string,
    professionalId?: number,
    page: number = 0,
    size: number = 100
  ): Observable<PageResponse<Appointment>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (searchTerm) params = params.set('searchTerm', searchTerm);
    if (status && status !== 'ALL') params = params.set('status', status);
    if (professionalId) params = params.set('professionalId', professionalId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<PageResponse<Appointment>>(`${this.apiUrl}/search`, { params });
  }

  getByPatientId(patientId: number, page: number = 0, size: number = 100): Observable<PageResponse<Appointment>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Appointment>>(`${this.apiUrl}/patient/${patientId}`, { params });
  }

  create(appointment: Appointment): Observable<Appointment> {
    return this.http.post<Appointment>(this.apiUrl, appointment);
  }

  update(id: number, appointment: Appointment, updateSeries: boolean = false): Observable<Appointment> {
    let params = new HttpParams();
    if (updateSeries) {
      params = params.set('updateSeries', 'true');
    }
    return this.http.put<Appointment>(`${this.apiUrl}/${id}`, appointment, { params });
  }

  updateStatus(id: number, status: string, updateSeries: boolean = false): Observable<Appointment> {
    let params = new HttpParams();
    if (updateSeries) {
      params = params.set('updateSeries', 'true');
    }
    return this.http.put<Appointment>(`${this.apiUrl}/${id}/status`, { status }, { params });
  }

  cancel(id: number, cancelSeries: boolean = false): Observable<Appointment> {
    let params = new HttpParams();
    if (cancelSeries) {
      params = params.set('cancelSeries', 'true');
    }
    return this.http.delete<Appointment>(`${this.apiUrl}/${id}`, { params });
  }

  getConfirmation(token: string): Observable<PublicAppointmentConfirmation> {
    return this.http.get<PublicAppointmentConfirmation>(`${environment.apiUrl}/v1/public/appointments/confirm/${token}`);
  }

  confirm(token: string): Observable<PublicAppointmentConfirmation> {
    return this.http.post<PublicAppointmentConfirmation>(
      `${environment.apiUrl}/v1/public/appointments/confirm/${token}`,
      null
    );
  }
}
