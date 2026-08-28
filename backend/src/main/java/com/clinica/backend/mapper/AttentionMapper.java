package com.clinica.backend.mapper;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.model.Attention;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo Entity↔DTO de {@link Attention}, generado en compilación por MapStruct.
 * Sustituye al método privado {@code mapToDto} que antes vivía dentro de
 * {@code AttentionService}. MapStruct genera automáticamente las
 * comprobaciones de nulidad para cada relación (paciente, profesional, cita,
 * sesión clínica, receta, pago y servicio clínico), igual que hacía el
 * mapeo manual.
 */
@Mapper(componentModel = "spring")
public interface AttentionMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "patient.fullName", target = "patientName")
    @Mapping(source = "patient.identificationDocument", target = "patientDocumentNumber")
    @Mapping(source = "professional.id", target = "professionalId")
    @Mapping(source = "professional.fullName", target = "professionalName")
    @Mapping(source = "appointment.id", target = "appointmentId")
    @Mapping(source = "clinicalSession.id", target = "clinicalSessionId")
    @Mapping(source = "prescription.id", target = "prescriptionId")
    @Mapping(source = "payment.id", target = "paymentId")
    @Mapping(source = "payment.status", target = "paymentStatus")
    @Mapping(source = "payment.amount", target = "paymentAmount")
    @Mapping(source = "clinicalService.id", target = "clinicalServiceId")
    @Mapping(source = "clinicalService.name", target = "clinicalServiceName")
    AttentionDto toDto(Attention attention);
}
