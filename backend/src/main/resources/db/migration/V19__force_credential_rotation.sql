-- La clave JWT anterior deja de ser válida al desplegar con un JWT_SECRET nuevo.
-- Obliga a renovar las contraseñas y revoca todas las sesiones emitidas antes del despliegue.
UPDATE users
SET must_change_password = TRUE,
    token_version = token_version + 1;

UPDATE refresh_tokens
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL;
