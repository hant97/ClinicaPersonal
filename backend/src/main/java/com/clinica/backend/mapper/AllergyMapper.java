package com.clinica.backend.mapper;

import com.clinica.backend.dto.AllergyDto;
import com.clinica.backend.model.Allergy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo Entity↔DTO de {@link Allergy}, generado en compilación por MapStruct.
 * Sustituye al método privado {@code mapToDto} que antes vivía dentro de
 * {@code AllergyService}.
 */
@Mapper(componentModel = "spring")
public interface AllergyMapper {

    @Mapping(source = "patient.id", target = "patientId")
    AllergyDto toDto(Allergy allergy);
}
