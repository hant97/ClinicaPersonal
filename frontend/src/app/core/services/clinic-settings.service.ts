import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ClinicSettings {
  id?: number;
  clinicName: string;
  shortName: string;
  logoUrl: string;
  contactEmail: string;
  contactPhone: string;
  address: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClinicSettingsService {
  private apiUrl = `${environment.apiUrl}/settings/clinic`;
  
  private settingsSubject = new BehaviorSubject<ClinicSettings>({
    clinicName: 'Cargando...',
    shortName: '...',
    logoUrl: '',
    contactEmail: '',
    contactPhone: '',
    address: ''
  });

  public settings$ = this.settingsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadSettings();
  }

  loadSettings(): void {
    this.http.get<ClinicSettings>(this.apiUrl).subscribe({
      next: (settings) => {
        if(settings) {
           this.settingsSubject.next(settings);
        }
      },
      error: (err) => console.error('Error loading clinic settings', err)
    });
  }

  getSettings(): Observable<ClinicSettings> {
    return this.http.get<ClinicSettings>(this.apiUrl);
  }

  updateSettings(settings: ClinicSettings): Observable<ClinicSettings> {
    return this.http.put<ClinicSettings>(this.apiUrl, settings).pipe(
      tap(updated => this.settingsSubject.next(updated))
    );
  }

  uploadLogo(file: File): Observable<ClinicSettings> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ClinicSettings>(`${this.apiUrl}/logo`, formData).pipe(
      tap(updated => this.settingsSubject.next(updated))
    );
  }

  getLogoUrl(logoPath: string): string {
    if (!logoPath) return '';
    if (logoPath.startsWith('http')) return logoPath;
    const baseUrl = environment.apiUrl.replace('/api', '');
    return baseUrl + logoPath;
  }
}
