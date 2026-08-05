---
name: buenas-practicas-proyecto-personalizado
description: Aplicar reglas generales de coherencia y calidad al modificar ClinicaPersonal con Java 17, Spring Boot 4 y Angular 18. Usar en cambios que atraviesen varias áreas, revisiones generales de arquitectura, limpieza técnica o cuando se necesite decidir qué skill específica del backend, frontend o contrato full-stack debe guiar la implementación.
---

# Mantener la coherencia general

## Elegir la guía específica

- Usar `$clinica-backend-feature` para cambios confinados al backend.
- Usar `$clinica-frontend-feature` para cambios confinados al frontend.
- Usar `$clinica-fullstack-contract` para cualquier cambio que altere una API o su consumidor.

## Aplicar reglas comunes

- Mantener nombres de código, clases, métodos y propiedades en inglés.
- Mantener textos visibles para usuarios en español.
- Respetar la estructura y el estilo de archivos vecinos.
- Preferir cambios pequeños, cohesivos y compatibles con el comportamiento existente.
- Usar imports explícitos; no escribir nombres de clase completamente cualificados dentro del
  cuerpo del código.
- No añadir dependencias, frameworks de estado, mapeadores ni capas arquitectónicas sin una
  necesidad demostrable.
- No copiar deuda técnica encontrada en archivos antiguos como si fuera una convención.
- Añadir pruebas de comportamiento proporcionales al riesgo.
- Ejecutar las pruebas y compilaciones de todas las aplicaciones afectadas.

## Cerrar el cambio

1. Revisar el diff completo.
2. Confirmar que no se modificaron secretos ni archivos generados.
3. Confirmar que backend y frontend conservan el mismo contrato.
4. Informar qué se verificó y cualquier deuda o riesgo que permanezca.
