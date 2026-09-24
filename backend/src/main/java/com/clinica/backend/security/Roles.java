package com.clinica.backend.security;

import java.util.Set;

/**
 * Roles del sistema. Son combinables: por ejemplo, un psicólogo que además administra la
 * clínica tiene {@link #PROFESIONAL} y {@link #ADMIN}. La especialidad (Psicología,
 * Dermatología) es un atributo del usuario, no un rol.
 * <ul>
 *   <li>{@link #PROFESIONAL}: profesional de salud; único rol con acceso a la información
 *   clínica y el que aparece en la agenda.</li>
 *   <li>{@link #ADMIN}: administra la clínica de su especialidad (usuarios, cobros,
 *   inventario, catálogos, eliminación de pacientes y cobros). Solo con {@link #PROFESIONAL}
 *   además puede ver o gestionar registros clínicos de otros profesionales.</li>
 *   <li>{@link #ASISTENTE}: recepción; pacientes, citas y cobros, sin información clínica.</li>
 *   <li>{@link #SITE_ADMIN}: administración del sitio web público; sin acceso clínico.</li>
 * </ul>
 */
public final class Roles {

    public static final String ADMIN = "ROLE_ADMIN";
    public static final String PROFESIONAL = "ROLE_PROFESIONAL";
    public static final String ASISTENTE = "ROLE_ASISTENTE";
    public static final String SITE_ADMIN = "ROLE_SITE_ADMIN";

    public static final Set<String> ALL = Set.of(ADMIN, PROFESIONAL, ASISTENTE, SITE_ADMIN);

    private Roles() {
    }
}
