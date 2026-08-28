package com.clinica.backend.mapper;

import com.clinica.backend.dto.PrescriptionItemDto;
import com.clinica.backend.model.PrescriptionItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PrescriptionItemMapper {

    PrescriptionItemDto toDto(PrescriptionItem item);
}
