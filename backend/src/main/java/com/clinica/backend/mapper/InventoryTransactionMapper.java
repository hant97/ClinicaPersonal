package com.clinica.backend.mapper;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.model.InventoryTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryTransactionMapper {

    @Mapping(source = "supply.id", target = "supplyId")
    @Mapping(source = "supply.name", target = "supplyName")
    InventoryTransactionDto toDto(InventoryTransaction transaction);
}
