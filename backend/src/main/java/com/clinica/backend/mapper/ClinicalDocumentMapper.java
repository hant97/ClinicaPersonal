package com.clinica.backend.mapper;

import com.clinica.backend.dto.ClinicalDocumentDto;
import com.clinica.backend.model.ClinicalDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClinicalDocumentMapper {

    @Mapping(source = "patient.id", target = "patientId")
    ClinicalDocumentDto toDto(ClinicalDocument document);
}
