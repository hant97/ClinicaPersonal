package com.clinica.backend.mapper;

import com.clinica.backend.dto.PsychologyEvaluationDto;
import com.clinica.backend.model.PsychologyEvaluation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PsychologyEvaluationMapper {

    @Mapping(source = "patient.id", target = "patientId")
    PsychologyEvaluationDto toDto(PsychologyEvaluation evaluation);
}
