package com.clinica.backend.mapper;

import com.clinica.backend.dto.ProcedureDto;
import com.clinica.backend.model.Procedure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProcedureMapper {

    @Mapping(source = "patient.id", target = "patientId")
    ProcedureDto toDto(Procedure procedure);
}
