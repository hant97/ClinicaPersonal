const CLINICAL_DRAFT_PREFIX = 'flowgrid_draft_session_';

/**
 * Clave del borrador de una sesión clínica. Incluye al usuario autenticado para que, en un
 * equipo compartido, un profesional no vea el borrador de otro.
 */
export function clinicalDraftKey(username: string | null, patientId: string | number): string {
  return `${CLINICAL_DRAFT_PREFIX}${username ?? 'anonimo'}_${patientId}`;
}

/**
 * Elimina todos los borradores clínicos del navegador, incluidos los guardados con el
 * formato anterior (sin usuario en la clave).
 */
export function clearClinicalDrafts(): void {
  try {
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const key = localStorage.key(i);
      if (key?.startsWith(CLINICAL_DRAFT_PREFIX)) {
        localStorage.removeItem(key);
      }
    }
  } catch {
    // Almacenamiento no disponible (modo privado o bloqueado): no hay borradores que limpiar.
  }
}
