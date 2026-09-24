/**
 * Roles del sistema (espejo de `Roles.java`). Son combinables: por ejemplo, una psicóloga que
 * además administra la clínica tiene PROFESIONAL y ADMIN. La especialidad es un atributo del
 * usuario, no un rol.
 */
export const ROLES = {
  PROFESIONAL: 'ROLE_PROFESIONAL',
  ADMIN: 'ROLE_ADMIN',
  ASISTENTE: 'ROLE_ASISTENTE',
  SITE_ADMIN: 'ROLE_SITE_ADMIN'
} as const;

export interface RoleOption {
  code: string;
  label: string;
  description: string;
  badgeClass: string;
}

/** Roles que un administrador de la clínica puede asignar desde la gestión de usuarios. */
export const ASSIGNABLE_ROLES: RoleOption[] = [
  {
    code: ROLES.PROFESIONAL,
    label: 'Profesional de salud',
    description: 'Atiende pacientes y accede a la historia clínica de su especialidad.',
    badgeClass: 'bg-primary-50 text-primary-700 border-primary-200'
  },
  {
    code: ROLES.ADMIN,
    label: 'Administrador',
    description: 'Gestiona usuarios, cobros, inventario y catálogos; puede eliminar pacientes y cobros.',
    badgeClass: 'bg-violet-50 text-violet-700 border-violet-200'
  },
  {
    code: ROLES.ASISTENTE,
    label: 'Asistente / Recepción',
    description: 'Gestiona pacientes, citas y cobros, sin acceso a información clínica.',
    badgeClass: 'bg-slate-100 text-slate-700 border-slate-200'
  }
];

const SITE_ADMIN_OPTION: RoleOption = {
  code: ROLES.SITE_ADMIN,
  label: 'Administrador web',
  description: 'Administra el sitio web público.',
  badgeClass: 'bg-amber-50 text-amber-700 border-amber-200'
};

/** Roles del usuario en orden estable, para mostrarlos como etiquetas. */
export function describeRoles(roles: readonly string[] | undefined): RoleOption[] {
  return [...ASSIGNABLE_ROLES, SITE_ADMIN_OPTION].filter(option => roles?.includes(option.code));
}

/** Misma regla que el backend: al menos un rol y Profesional no se combina con Asistente. */
export function roleSelectionError(roles: readonly string[] | undefined): string | null {
  if (!roles || roles.length === 0) {
    return 'Seleccione al menos un rol.';
  }
  if (roles.includes(ROLES.PROFESIONAL) && roles.includes(ROLES.ASISTENTE)) {
    return 'Un profesional ya puede gestionar pacientes, citas y cobros: no combine Profesional con Asistente.';
  }
  return null;
}
