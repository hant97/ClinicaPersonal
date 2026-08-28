package com.clinica.backend.mapper;

import com.clinica.backend.dto.EvolutionDto;
import com.clinica.backend.model.Evolution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EvolutionMapper {

    @Mapping(source = "patient.id", target = "patientId")
    EvolutionDto toDto(Evolution evolution);
}
