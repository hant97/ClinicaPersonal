-- Usuarios iniciales. Las tres cuentas deben cambiar su contraseña en el primer acceso.
INSERT INTO users (username, password, specialty, must_change_password, token_version)
VALUES
    ('admin', '$2a$10$I9dHLFAtRUVzcA8ngzcITeN1q0R2SBfEry0THN8JPmMYwF0b6eAKW', 'PSICOLOGIA', TRUE, 0),
    ('admin_psico', '$2a$10$se0lzYQLOIPq7tlmDCKmjeru.B7wY9fiwy38y3ysbByq/EmMPI0zW', 'PSICOLOGIA', TRUE, 0),
    ('admin_derm', '$2a$10$7/MDrYR55wnkGdflK3n9NuFtGhYoONMU.vkRNn4Ox1i96xhtYNC82', 'DERMATOLOGIA', TRUE, 0)
ON CONFLICT (username) DO NOTHING;

-- El usuario del sitio no debe conservar permisos de administración clínica.
DELETE FROM user_roles
WHERE user_id = (SELECT id FROM users WHERE username = 'admin')
  AND role = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role)
SELECT u.id, roles.role
FROM users u
JOIN (VALUES
    ('admin', 'ROLE_SITE_ADMIN'),
    ('admin_psico', 'ROLE_ADMIN'),
    ('admin_derm', 'ROLE_ADMIN')
) AS roles(username, role) ON roles.username = u.username
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role = roles.role
);
