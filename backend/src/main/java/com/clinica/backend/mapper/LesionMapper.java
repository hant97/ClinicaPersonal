package com.clinica.backend.mapper;

import com.clinica.backend.dto.LesionDto;
import com.clinica.backend.model.Lesion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LesionMapper {

    @Mapping(source = "patient.id", target = "patientId")
    LesionDto toDto(Lesion lesion);
}
