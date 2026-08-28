package com.clinica.backend.mapper;

import com.clinica.backend.dto.ClinicSettingsDto;
import com.clinica.backend.model.ClinicSettings;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClinicSettingsMapper {

    ClinicSettingsDto toDto(ClinicSettings entity);
}
