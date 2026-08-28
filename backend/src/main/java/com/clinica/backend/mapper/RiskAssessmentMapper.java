package com.clinica.backend.mapper;

import com.clinica.backend.dto.RiskAssessmentDto;
import com.clinica.backend.model.RiskAssessment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RiskAssessmentMapper {

    RiskAssessmentDto toDto(RiskAssessment entity);
}
