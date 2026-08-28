package com.clinica.backend.mapper;

import com.clinica.backend.dto.PaymentTransactionDto;
import com.clinica.backend.model.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentTransactionMapper {

    @Mapping(source = "payment.id", target = "paymentId")
    PaymentTransactionDto toDto(PaymentTransaction transaction);
}
