package com.clinica.backend.mapper;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code toDto} ignora {@code hasActiveAlerts}: no es un campo de la entidad,
 * sino un valor calculado que {@code PatientService} resuelve aparte con una
 * consulta a las alertas de riesgo.
 * <p>
 * {@code toEntity} ignora {@code uuid}, {@code photoUrl}, {@code active} y
 * {@code specialty}: el service les aplica reglas propias (mantener el uuid
 * generado si el DTO no trae uno, recortar la URL de foto, el valor por
 * defecto de {@code active} y la especialidad del usuario autenticado) en
 * vez de copiarlos de forma directa.
 */
@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "hasActiveAlerts", ignore = true)
    PatientDto toDto(Patient patient);

    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "photoUrl", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    Patient toEntity(PatientDto dto);
}
