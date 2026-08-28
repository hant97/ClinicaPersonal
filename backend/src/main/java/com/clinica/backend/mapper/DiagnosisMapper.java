package com.clinica.backend.mapper;

import com.clinica.backend.dto.DiagnosisDto;
import com.clinica.backend.model.Diagnosis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DiagnosisMapper {

    @Mapping(source = "patient.id", target = "patientId")
    DiagnosisDto toDto(Diagnosis diagnosis);
}
