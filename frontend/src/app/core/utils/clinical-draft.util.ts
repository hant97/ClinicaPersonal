const CLINICAL_DRAFT_PREFIX = 'flowgrid_draft_session_';

export interface LegacyClinicalDraft {
  storageKey: string;
  patientId: number;
  content: Record<string, unknown>;
}

/**
 * Clave del borrador de una sesión clínica. Incluye al usuario autenticado para que, en un
 * equipo compartido, un profesional no vea el borrador de otro.
 */
export function clinicalDraftKey(username: string | null, patientId: string | number): string {
  return `${CLINICAL_DRAFT_PREFIX}${username ?? 'anonimo'}_${patientId}`;
}

/** Lee los borradores locales de la versión anterior para migrarlos al servidor al iniciar sesión. */
export function getLegacyClinicalDrafts(username: string): LegacyClinicalDraft[] {
  const keyPrefix = `${CLINICAL_DRAFT_PREFIX}${username}_`;
  const drafts: LegacyClinicalDraft[] = [];

  try {
    const storageKeys: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key) storageKeys.push(key);
    }

    for (const storageKey of storageKeys) {
      if (!storageKey?.startsWith(keyPrefix)) continue;

      const patientIdValue = storageKey.slice(keyPrefix.length);
      if (!/^\d+$/.test(patientIdValue)) continue;

      const rawContent = localStorage.getItem(storageKey);
      if (!rawContent) continue;

      try {
        const content: unknown = JSON.parse(rawContent);
        if (content && typeof content === 'object' && !Array.isArray(content)) {
          drafts.push({ storageKey, patientId: Number(patientIdValue), content: content as Record<string, unknown> });
        }
      } catch {
        removeLegacyClinicalDraft(storageKey);
      }
    }
  } catch {
    // El almacenamiento local puede estar deshabilitado por el navegador.
  }

  return drafts;
}

export function removeLegacyClinicalDraft(storageKey: string): void {
  try {
    localStorage.removeItem(storageKey);
  } catch {
    // El almacenamiento local puede estar deshabilitado por el navegador.
  }
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
