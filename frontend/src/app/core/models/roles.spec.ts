import { ROLES, describeRoles, roleSelectionError } from './roles';

describe('roles', () => {
  it('acepta combinar Profesional con Administrador', () => {
    expect(roleSelectionError([ROLES.PROFESIONAL, ROLES.ADMIN])).toBeNull();
    expect(roleSelectionError([ROLES.ASISTENTE, ROLES.ADMIN])).toBeNull();
  });

  it('exige al menos un rol y rechaza Profesional con Asistente', () => {
    expect(roleSelectionError([])).not.toBeNull();
    expect(roleSelectionError(undefined)).not.toBeNull();
    expect(roleSelectionError([ROLES.PROFESIONAL, ROLES.ASISTENTE])).not.toBeNull();
  });

  it('describe los roles en un orden estable, incluido el administrador web', () => {
    const labels = describeRoles([ROLES.SITE_ADMIN, ROLES.ADMIN, ROLES.PROFESIONAL]).map(role => role.label);

    expect(labels).toEqual(['Profesional de salud', 'Administrador', 'Administrador web']);
  });
});
