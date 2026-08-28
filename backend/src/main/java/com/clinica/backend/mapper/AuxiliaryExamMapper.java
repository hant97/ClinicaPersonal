package com.clinica.backend.mapper;

import com.clinica.backend.dto.AuxiliaryExamDto;
import com.clinica.backend.model.AuxiliaryExam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuxiliaryExamMapper {

    @Mapping(source = "patient.id", target = "patientId")
    AuxiliaryExamDto toDto(AuxiliaryExam exam);
}
