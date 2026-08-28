package com.clinica.backend.mapper;

import com.clinica.backend.dto.DermatologicalHistoryDto;
import com.clinica.backend.model.DermatologicalHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DermatologicalHistoryMapper {

    @Mapping(source = "patient.id", target = "patientId")
    DermatologicalHistoryDto toDto(DermatologicalHistory history);
}
