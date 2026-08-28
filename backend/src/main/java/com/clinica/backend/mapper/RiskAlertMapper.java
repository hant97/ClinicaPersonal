package com.clinica.backend.mapper;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.model.RiskAlert;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RiskAlertMapper {

    RiskAlertDto toDto(RiskAlert entity);
}
