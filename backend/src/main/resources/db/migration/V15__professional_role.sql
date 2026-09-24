-- Separa "profesional de salud" de "administrador". Hasta ahora ROLE_ADMIN significaba
-- "Profesional Titular (Admin)", así que cada administrador existente conserva su acceso
-- clínico recibiendo también ROLE_PROFESIONAL. Los asistentes (ROLE_ASISTENTE) y el
-- administrador del sitio (ROLE_SITE_ADMIN) no reciben el rol: no acceden a datos clínicos.
INSERT INTO user_roles (user_id, role)
SELECT DISTINCT admin_role.user_id, 'ROLE_PROFESIONAL'
FROM user_roles admin_role
WHERE admin_role.role = 'ROLE_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles existing
      WHERE existing.user_id = admin_role.user_id
        AND existing.role = 'ROLE_PROFESIONAL'
  );
