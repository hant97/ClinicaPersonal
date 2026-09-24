package com.clinica.backend.mapper;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code toDto} ignora {@code hasActiveAlerts} y {@code photoUrl}: no son campos
 * de la entidad, sino valores que {@code PatientService} calcula aparte (una
 * consulta a las alertas de riesgo y la URL autenticada de la foto).
 * <p>
 * {@code toEntity} ignora {@code uuid}, {@code photoKey}, {@code active} y
 * {@code specialty}: el service les aplica reglas propias (mantener el uuid
 * generado si el DTO no trae uno, la foto solo cambia mediante su endpoint de
 * subida, el valor por defecto de {@code active} y la especialidad del usuario
 * autenticado) en vez de copiarlos de forma directa.
 */
@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "hasActiveAlerts", ignore = true)
    @Mapping(target = "photoUrl", ignore = true)
    PatientDto toDto(Patient patient);

    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "photoKey", ignore = true)
    @Mapping(target = "searchText", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    Patient toEntity(PatientDto dto);
}
