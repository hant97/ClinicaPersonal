package com.clinica.backend.mapper;

import com.clinica.backend.dto.GeneralHistoryDto;
import com.clinica.backend.model.GeneralHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GeneralHistoryMapper {

    @Mapping(source = "patient.id", target = "patientId")
    GeneralHistoryDto toDto(GeneralHistory history);
}
