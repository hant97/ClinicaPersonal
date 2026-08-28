package com.clinica.backend.mapper;

import com.clinica.backend.dto.PrescriptionDto;
import com.clinica.backend.model.Prescription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapea los campos directos de {@link Prescription}. {@code items} se arma
 * aparte en {@code PrescriptionService} para conservar el manejo explícito
 * de listas nulas.
 */
@Mapper(componentModel = "spring")
public interface PrescriptionMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(target = "items", ignore = true)
    PrescriptionDto toDto(Prescription prescription);
}
