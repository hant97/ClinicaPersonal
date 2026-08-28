package com.clinica.backend.mapper;

import com.clinica.backend.dto.ScheduleBlockDto;
import com.clinica.backend.model.ScheduleBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapea los campos directos de {@link ScheduleBlock}. {@code professionalName}
 * se completa aparte en {@code ScheduleBlockService} porque depende de una
 * regla de negocio ("Toda la especialidad" cuando no hay profesional
 * asignado), no de una copia directa de campos.
 */
@Mapper(componentModel = "spring")
public interface ScheduleBlockMapper {

    @Mapping(target = "professionalName", ignore = true)
    ScheduleBlockDto toDto(ScheduleBlock block);
}
