package com.clinica.backend.mapper;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.model.ClinicalService;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClinicalServiceMapper {

    ClinicalServiceDto toDto(ClinicalService service);
}
