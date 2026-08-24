import { Injectable } from '@angular/core';

export const VIEWPORT_BREAKPOINTS = {
  mobile: 768,
  compact: 1024,
  expandedSidebar: 1280
} as const;

@Injectable({
  providedIn: 'root'
})
export class ViewPreferenceService {
  isMobile(): boolean {
    if (typeof window === 'undefined') return false;
    return window.innerWidth < VIEWPORT_BREAKPOINTS.mobile;
  }

  isCompact(): boolean {
    if (typeof window === 'undefined') return false;
    return window.innerWidth < VIEWPORT_BREAKPOINTS.compact;
  }

  canExpandSidebar(): boolean {
    if (typeof window === 'undefined') return true;
    return window.innerWidth >= VIEWPORT_BREAKPOINTS.expandedSidebar;
  }

  getViewMode<T extends string>(storageKey: string, defaultDesktop: T, defaultCompact: T): T {
    if (typeof window === 'undefined') return defaultDesktop;
    try {
      const saved = localStorage.getItem(storageKey) as T | null;
      if (saved) return saved;
    } catch {
      // Ignorar errores al acceder a localStorage
    }
    return this.isCompact() ? defaultCompact : defaultDesktop;
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
