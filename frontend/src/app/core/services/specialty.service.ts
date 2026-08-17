import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay, tap } from 'rxjs';
import { AuthService } from './auth.service';
import { SpecialtyItem } from '../models/specialty.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SpecialtyService {
  private apiUrl = `${environment.apiUrl}/v1/specialties`;
  private activeSpecialties$?: Observable<SpecialtyItem[]>;

  constructor(
    private authService: AuthService,
    private http: HttpClient
  ) { }

  /**
   * Obtiene la lista de especialidades activas desde la API con caché reactivo.
   */
  getActiveSpecialties(): Observable<SpecialtyItem[]> {
    if (!this.activeSpecialties$) {
      this.activeSpecialties$ = this.http.get<SpecialtyItem[]>(this.apiUrl).pipe(
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.activeSpecialties$;
  }

  /**
   * Obtiene todas las especialidades (activas e inactivas) para administración.
   */
  getAllSpecialties(): Observable<SpecialtyItem[]> {
    return this.http.get<SpecialtyItem[]>(`${this.apiUrl}/all`);
  }

  /**
   * Obtiene el detalle de una especialidad por su código.
   */
  getSpecialtyByCode(code: string): Observable<SpecialtyItem> {
    return this.http.get<SpecialtyItem>(`${this.apiUrl}/${code}`);
  }

  /**
   * Registra una nueva especialidad en el catálogo maestro.
   */
  createSpecialty(specialty: Partial<SpecialtyItem>): Observable<SpecialtyItem> {
    return this.http.post<SpecialtyItem>(this.apiUrl, specialty).pipe(
      tap(() => this.clearCache())
    );
  }

  /**
   * Actualiza los datos de una especialidad.
   */
  updateSpecialty(id: number, specialty: Partial<SpecialtyItem>): Observable<SpecialtyItem> {
    return this.http.put<SpecialtyItem>(`${this.apiUrl}/${id}`, specialty).pipe(
      tap(() => this.clearCache())
    );
  }

  /**
   * Habilita o deshabilita una especialidad.
   */
  toggleActive(id: number, active: boolean): Observable<SpecialtyItem> {
    return this.http.patch<SpecialtyItem>(`${this.apiUrl}/${id}/toggle-active`, null, {
      params: { active: active.toString() }
    }).pipe(
      tap(() => this.clearCache())
    );
  }

  /**
   * Limpia la caché en memoria de especialidades activas.
   */
  clearCache(): void {
    this.activeSpecialties$ = undefined;
  }

  // ==========================================
  // MÉTODOS DE CONTEXTO DE USUARIO AUTENTICADO
  // ==========================================

  getSpecialty(): 'PSICOLOGIA' | 'DERMATOLOGIA' | string | null {
    const token = this.authService.getToken();
    if (!token) return null;

    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;
      const payload = parts[1];
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
      const jsonString = decodeURIComponent(
        atob(padded)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      const decoded = JSON.parse(jsonString);
      return (decoded.specialty as string) || null;
    } catch (e) {
      console.error('Error decoding JWT for specialty', e);
      return null;
    }
  }

  isPsychology(): boolean {
    return this.getSpecialty() === 'PSICOLOGIA';
  }

  isDermatology(): boolean {
    return this.getSpecialty() === 'DERMATOLOGIA';
  }
}
