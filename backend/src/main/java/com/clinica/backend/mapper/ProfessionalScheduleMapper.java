package com.clinica.backend.mapper;

import com.clinica.backend.dto.ProfessionalScheduleDto;
import com.clinica.backend.model.ProfessionalSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfessionalScheduleMapper {

    ProfessionalScheduleDto toDto(ProfessionalSchedule entity);
}
