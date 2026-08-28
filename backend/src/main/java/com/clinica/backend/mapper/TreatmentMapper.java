package com.clinica.backend.mapper;

import com.clinica.backend.dto.TreatmentDto;
import com.clinica.backend.model.Treatment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TreatmentMapper {

    @Mapping(source = "patient.id", target = "patientId")
    TreatmentDto toDto(Treatment treatment);
}
