package com.clinica.backend.mapper;

import com.clinica.backend.dto.PaymentItemDto;
import com.clinica.backend.model.PaymentItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentItemMapper {

    @Mapping(source = "payment.id", target = "paymentId")
    @Mapping(source = "supply.id", target = "supplyId")
    @Mapping(source = "clinicalService.id", target = "clinicalServiceId")
    PaymentItemDto toDto(PaymentItem item);
}
