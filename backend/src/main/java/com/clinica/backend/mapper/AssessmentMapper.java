package com.clinica.backend.mapper;

import com.clinica.backend.dto.AssessmentDto;
import com.clinica.backend.model.Assessment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssessmentMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "psychometricTest.id", target = "psychometricTestId")
    @Mapping(source = "psychometricTest.name", target = "testName")
    @Mapping(source = "psychometricTest.interpretationJson", target = "interpretationJson")
    AssessmentDto toDto(Assessment assessment);
}
