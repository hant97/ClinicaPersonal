package com.clinica.backend.mapper;

import com.clinica.backend.dto.CatalogItemDto;
import com.clinica.backend.model.CatalogItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogItemMapper {

    @Mapping(source = "catalog.id", target = "catalogId")
    CatalogItemDto toDto(CatalogItem item);
}
