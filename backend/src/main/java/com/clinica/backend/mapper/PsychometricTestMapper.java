package com.clinica.backend.mapper;

import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.model.PsychometricTest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PsychometricTestMapper {

    PsychometricTestDto toDto(PsychometricTest test);
}
