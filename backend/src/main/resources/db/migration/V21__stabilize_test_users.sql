-- Mantiene tres cuentas de prueba estables, una por perfil funcional.
INSERT INTO users (username, password, specialty, must_change_password, token_version, enabled)
VALUES
    ('admin', '$2a$10$I9dHLFAtRUVzcA8ngzcITeN1q0R2SBfEry0THN8JPmMYwF0b6eAKW', 'PSICOLOGIA', FALSE, 0, TRUE),
    ('admin_psico', '$2a$10$se0lzYQLOIPq7tlmDCKmjeru.B7wY9fiwy38y3ysbByq/EmMPI0zW', 'PSICOLOGIA', FALSE, 0, TRUE),
    ('admin_derm', '$2a$10$7/MDrYR55wnkGdflK3n9NuFtGhYoONMU.vkRNn4Ox1i96xhtYNC82', 'DERMATOLOGIA', FALSE, 0, TRUE)
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    specialty = EXCLUDED.specialty,
    must_change_password = FALSE,
    token_version = users.token_version + 1,
    enabled = TRUE;

DELETE FROM user_roles
WHERE user_id IN (
    SELECT id FROM users WHERE username IN ('admin', 'admin_psico', 'admin_derm')
);

INSERT INTO user_roles (user_id, role)
SELECT u.id, assigned.role
FROM users u
JOIN (VALUES
    ('admin', 'ROLE_SITE_ADMIN'),
    ('admin_psico', 'ROLE_ADMIN'),
    ('admin_derm', 'ROLE_ADMIN')
) AS assigned(username, role) ON assigned.username = u.username;

UPDATE refresh_tokens
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL
  AND user_id IN (
      SELECT id FROM users WHERE username IN ('admin', 'admin_psico', 'admin_derm')
  );

ALTER TABLE users DROP COLUMN IF EXISTS must_change_password;
