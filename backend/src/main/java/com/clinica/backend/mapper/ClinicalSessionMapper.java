package com.clinica.backend.mapper;

import com.clinica.backend.dto.ClinicalSessionDto;
import com.clinica.backend.model.ClinicalSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClinicalSessionMapper {

    @Mapping(source = "patient.id", target = "patientId")
    ClinicalSessionDto toDto(ClinicalSession entity);
}
