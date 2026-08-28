package com.clinica.backend.mapper;

import com.clinica.backend.dto.MedicationDto;
import com.clinica.backend.model.Medication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MedicationMapper {

    @Mapping(source = "patient.id", target = "patientId")
    MedicationDto toDto(Medication medication);
}
