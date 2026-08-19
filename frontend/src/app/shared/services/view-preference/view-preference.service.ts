import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ViewPreferenceService {
  private readonly MOBILE_BREAKPOINT = 768;

  isMobile(): boolean {
    if (typeof window === 'undefined') return false;
    return window.innerWidth < this.MOBILE_BREAKPOINT;
  }

  getViewMode<T extends string>(storageKey: string, defaultDesktop: T, defaultMobile: T): T {
    if (typeof window === 'undefined') return defaultDesktop;
    try {
      const saved = localStorage.getItem(storageKey) as T | null;
      if (saved) return saved;
    } catch {
      // Ignorar errores al acceder a localStorage
    }
    return this.isMobile() ? defaultMobile : defaultDesktop;
  }

  setViewMode<T extends string>(storageKey: string, mode: T): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(storageKey, mode);
    } catch {
      // Ignorar cuota o restricciones de almacenamiento en modo incógnito
    }
  }
}
