package com.clinica.backend.mapper;

import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.model.Supply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code toEntity} ignora {@code imageUrl} y {@code specialty}: el service
 * les aplica una regla propia (recortar la URL a {@code null} si queda vacía,
 * y solo sobrescribir la especialidad si el DTO trae una) en vez de copiarlos
 * de forma directa.
 */
@Mapper(componentModel = "spring")
public interface SupplyMapper {

    SupplyDto toDto(Supply supply);

    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    Supply toEntity(SupplyDto dto);
}
