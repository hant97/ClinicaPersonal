import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

/**
 * Módulos que casi todos los componentes standalone del feature layer
 * repiten en su array `imports` (directivas estructurales de `*ngIf`/`*ngFor`
 * vía `CommonModule`, y binding de formularios reactivos vía
 * `ReactiveFormsModule`). En vez de repetir ambos símbolos en cada componente,
 * se puede hacer spread de esta constante: `imports: [...COMMON_STANDALONE_IMPORTS, ...]`.
 *
 * Un componente que no use formularios reactivos puede omitir este array y
 * seguir importando `CommonModule` solo — no es obligatorio adoptarlo.
 */
export const COMMON_STANDALONE_IMPORTS = [CommonModule, ReactiveFormsModule] as const;
