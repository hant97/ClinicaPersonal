package com.clinica.backend.mapper;

import com.clinica.backend.dto.CatalogDto;
import com.clinica.backend.model.Catalog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapea los campos escalares de {@link Catalog}. La lista de {@code items}
 * se arma aparte en {@code CatalogService} porque requiere ordenarlos por
 * {@code orderIndex} antes de mapearlos — no es una copia directa de campos.
 */
@Mapper(componentModel = "spring")
public interface CatalogMapper {

    @Mapping(target = "items", ignore = true)
    CatalogDto toDto(Catalog catalog);
}
