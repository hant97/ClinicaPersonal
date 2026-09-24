# Política de autorización clínica

## Roles

Los roles son **combinables**: un usuario puede ser, por ejemplo, psicólogo y administrador a
la vez (`ROLE_PROFESIONAL` + `ROLE_ADMIN`). La especialidad (Psicología, Dermatología) es un
atributo del usuario, no un rol. Definidos en `Roles.java` y `frontend/src/app/core/models/roles.ts`.

| Rol | Puede | No puede |
|---|---|---|
| `ROLE_PROFESIONAL` | Acceder a la información clínica de su especialidad; aparece en la agenda. | Eliminar pacientes ni cobros (salvo que también sea administrador). |
| `ROLE_ADMIN` | Gestionar usuarios, cobros, inventario, catálogos y servicios; **eliminar pacientes y cobros**. | Ver historias clínicas si no es también profesional. |
| `ROLE_ASISTENTE` | Gestionar pacientes (sin eliminarlos), citas, atenciones y cobros. | Acceder a información clínica. |
| `ROLE_SITE_ADMIN` | Administrar el sitio web público. | Acceder a información clínica. |

Reglas de asignación: al menos un rol por usuario; `ROLE_PROFESIONAL` no se combina con
`ROLE_ASISTENTE` (un profesional ya gestiona pacientes, citas y cobros); solo un
`ROLE_SITE_ADMIN` puede asignar `ROLE_SITE_ADMIN`. La migración `V15` asignó
`ROLE_PROFESIONAL` a los administradores existentes, porque hasta entonces `ROLE_ADMIN`
significaba "Profesional Titular (Admin)".

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

Toda la información clínica (incluidas las alertas de riesgo, documentos clínicos, recetas, evaluaciones psicométricas y la vinculación de sesiones o recetas a una atención) exige `ROLE_PROFESIONAL`. En la tabla, "profesional" significa usuario con ese rol, y un **administrador clínico** es un profesional que además tiene `ROLE_ADMIN` y cuya especialidad coincide con la del recurso. Un `ROLE_ADMIN` sin `ROLE_PROFESIONAL` no accede a datos clínicos. `ROLE_SITE_ADMIN` no concede acceso clínico. El indicador "tiene alertas activas" de la ficha del paciente sigue visible para recepción, sin el detalle de la alerta.

La eliminación (lógica) de pacientes y cobros, incluidos los abonos, exige `ROLE_ADMIN` mediante `@PreAuthorize` en `PatientController` y `PaymentController`. El acceso fuera de especialidad o sin propiedad devuelve `403` sin revelar detalles del registro. Una petición sin autenticación es rechazada por Spring Security con `401`; los registros borrados lógicamente responden `404`.

## Catálogo de servicios clínicos

El catálogo de servicios clínicos (`clinical_services`) es un recurso compartido por especialidad y su administración (crear, editar y borrar) está reservada exclusivamente a `ROLE_ADMIN` mediante `@PreAuthorize("hasRole('ADMIN')")` en el controlador. A diferencia de los registros clínicos (que admiten a profesionales de la misma especialidad), la lectura sigue el alcance por especialidad y la escritura es solo de administradores. Esto es intencional: el catálogo define precios, categorías y duración de los servicios, por lo que no se expone a profesionales no administradores.

## Retención y auditoría

Por trazabilidad clínica, sesiones, historias y evaluaciones no se borran físicamente. La eliminación registra `deleted`, `deleted_at` y `deleted_by`; las consultas operativas excluyen los registros eliminados. No existe endpoint para consultar registros eliminados: una futura consulta de auditoría deberá requerir explícitamente un rol clínico autorizado y conservar esta restricción por especialidad.
