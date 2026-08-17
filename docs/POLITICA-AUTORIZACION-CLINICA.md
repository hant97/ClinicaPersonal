# Política de autorización clínica

## Ámbito

Esta política cubre sesiones clínicas, evaluaciones psicológicas, evaluaciones dermatológicas y los bloques de la historia clínica: antecedentes generales, alergias, medicamentos, antecedentes dermatológicos, diagnósticos, planes terapéuticos, lesiones, exámenes auxiliares, tratamientos, procedimientos y controles/evoluciones. Las rutas REST existentes se mantienen; el servidor obtiene siempre el profesional desde el JWT. Los valores `professionalId` recibidos en un DTO son datos no confiables y no se usan para asignar ni autorizar registros.

## Acceso operativo

| Recurso | Lectura | Crear | Editar / borrar |
|---|---|---|---|
| Sesión no confidencial | Profesionales de la misma especialidad | Profesional autenticado de la especialidad | Creador o `ROLE_ADMIN` de la misma especialidad |
| Sesión confidencial | Creador o `ROLE_ADMIN` de la misma especialidad | Profesional autenticado de la especialidad | Creador o `ROLE_ADMIN` de la misma especialidad |
| Historia clínica (antecedentes generales, alergias, medicamentos, antecedentes dermatológicos, diagnósticos, planes terapéuticos, lesiones, exámenes auxiliares, tratamientos, procedimientos, controles) | Creador o `ROLE_ADMIN` de la misma especialidad | Profesional autenticado de la especialidad | Creador o `ROLE_ADMIN` de la misma especialidad |
| Evaluación psicológica | Creador o `ROLE_ADMIN` de Psicología | Profesional autenticado de Psicología | Creador o `ROLE_ADMIN` de Psicología |
| Evaluación dermatológica | Creador o `ROLE_ADMIN` de Dermatología | Profesional autenticado de Dermatología | Creador o `ROLE_ADMIN` de Dermatología |

Un administrador clínico es un usuario con `ROLE_ADMIN` cuya especialidad coincide con la del recurso. `ROLE_SITE_ADMIN` no concede acceso clínico. El acceso fuera de especialidad o sin propiedad devuelve `403` sin revelar detalles del registro. Una petición sin autenticación es rechazada por Spring Security con `401`; los registros borrados lógicamente responden `404`.

## Catálogo de servicios clínicos

El catálogo de servicios clínicos (`clinical_services`) es un recurso compartido por especialidad y su administración (crear, editar y borrar) está reservada exclusivamente a `ROLE_ADMIN` mediante `@PreAuthorize("hasRole('ADMIN')")` en el controlador. A diferencia de los registros clínicos (que admiten a profesionales de la misma especialidad), la lectura sigue el alcance por especialidad y la escritura es solo de administradores. Esto es intencional: el catálogo define precios, categorías y duración de los servicios, por lo que no se expone a profesionales no administradores.

## Retención y auditoría

Por trazabilidad clínica, sesiones, historias y evaluaciones no se borran físicamente. La eliminación registra `deleted`, `deleted_at` y `deleted_by`; las consultas operativas excluyen los registros eliminados. No existe endpoint para consultar registros eliminados: una futura consulta de auditoría deberá requerir explícitamente un rol clínico autorizado y conservar esta restricción por especialidad.
