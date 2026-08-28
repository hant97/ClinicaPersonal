package com.clinica.backend.mapper;

import com.clinica.backend.dto.TherapeuticPlanDto;
import com.clinica.backend.model.TherapeuticPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TherapeuticPlanMapper {

    @Mapping(source = "patient.id", target = "patientId")
    TherapeuticPlanDto toDto(TherapeuticPlan plan);
}
