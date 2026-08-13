import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SpecialtyService {

  constructor(private authService: AuthService) { }

  getSpecialty(): 'PSICOLOGIA' | 'DERMATOLOGIA' | null {
    const token = this.authService.getToken();
    if (!token) return null;

    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      return decoded.specialty as 'PSICOLOGIA' | 'DERMATOLOGIA' | null;
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
