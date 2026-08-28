import { Injectable } from '@angular/core';

const CHECKLIST_DISMISSED_KEY = 'onboarding_checklist_dismissed';
const SIDEBAR_SECTION_COLLAPSED_PREFIX = 'sidebar_section_';
const PATIENT_DETAIL_SEEN_KEY = 'onboarding_patient_detail_seen';

/**
 * Rastrea el progreso de onboarding de un profesional que usa el sistema por
 * primera vez (checklist de primeros pasos, secciones colapsadas por
 * defecto, banners contextuales vistos). Todo el estado vive en localStorage
 * del navegador: no hay nada que sincronizar con el backend.
 */
@Injectable({
  providedIn: 'root'
})
export class OnboardingService {
  isChecklistDismissed(): boolean {
    if (typeof window === 'undefined') return false;
    try {
      return localStorage.getItem(CHECKLIST_DISMISSED_KEY) === 'true';
    } catch {
      return false;
    }
  }

  dismissChecklist(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(CHECKLIST_DISMISSED_KEY, 'true');
    } catch {
      // Ignorar cuota o restricciones de almacenamiento en modo incógnito
    }
  }

  /**
   * Estado de colapso de un grupo de navegación del sidebar (ej. "Operación
   * & Recursos"). `defaultCollapsed` aplica solo la primera vez, antes de
   * que el usuario haya expresado una preferencia explícita.
   */
  isSidebarSectionCollapsed(sectionKey: string, defaultCollapsed: boolean): boolean {
    if (typeof window === 'undefined') return defaultCollapsed;
    try {
      const saved = localStorage.getItem(SIDEBAR_SECTION_COLLAPSED_PREFIX + sectionKey);
      if (saved !== null) return saved === 'true';
    } catch {
      // Ignorar errores al acceder a localStorage
    }
    return defaultCollapsed;
  }

  setSidebarSectionCollapsed(sectionKey: string, collapsed: boolean): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(SIDEBAR_SECTION_COLLAPSED_PREFIX + sectionKey, String(collapsed));
    } catch {
      // Ignorar cuota o restricciones de almacenamiento en modo incógnito
    }
  }

  /**
   * Indica si el médico ya vio, en alguna ficha de paciente, la explicación
   * de que las secciones del expediente son colapsables. Se marca una sola
   * vez de forma global (no por paciente).
   */
  isFirstPatientVisitSeen(): boolean {
    if (typeof window === 'undefined') return true;
    try {
      return localStorage.getItem(PATIENT_DETAIL_SEEN_KEY) === 'true';
    } catch {
      return true;
    }
  }

  markFirstPatientVisitSeen(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(PATIENT_DETAIL_SEEN_KEY, 'true');
    } catch {
      // Ignorar cuota o restricciones de almacenamiento en modo incógnito
    }
  }
}
