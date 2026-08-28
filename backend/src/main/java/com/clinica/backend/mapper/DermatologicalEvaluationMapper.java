package com.clinica.backend.mapper;

import com.clinica.backend.dto.DermatologicalEvaluationDto;
import com.clinica.backend.model.DermatologicalEvaluation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DermatologicalEvaluationMapper {

    @Mapping(source = "patient.id", target = "patientId")
    DermatologicalEvaluationDto toDto(DermatologicalEvaluation evaluation);
}
