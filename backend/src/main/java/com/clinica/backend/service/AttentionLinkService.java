package com.clinica.backend.service;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.AttentionMapper;
import com.clinica.backend.model.Attention;
import com.clinica.backend.model.ClinicalSession;
import com.clinica.backend.model.Payment;
import com.clinica.backend.model.Prescription;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AttentionRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttentionLinkService {
    private final AttentionRepository attentionRepository;
    private final ClinicalSessionRepository clinicalSessionRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PaymentRepository paymentRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final AuditLogService auditLogService;
    private final AttentionMapper attentionMapper;

    @Transactional
    public AttentionDto linkClinicalSession(Long attentionId, Long sessionId) {
        User user = clinicalAuthorizationService.currentProfessional();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(attentionId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + attentionId));

        ClinicalSession session = clinicalSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión clínica no encontrada con ID: " + sessionId));

        attention.setClinicalSession(session);
        session.setAttentionId(attention.getId());
        clinicalSessionRepository.save(session);
        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Sesión clínica ID: " + sessionId + " vinculada a Atención ID: " + attentionId
        );

        return attentionMapper.toDto(updated);
    }

    @Transactional
    public AttentionDto linkPrescription(Long attentionId, Long prescriptionId) {
        User user = clinicalAuthorizationService.currentProfessional();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(attentionId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + attentionId));

        Prescription prescription = prescriptionRepository.findByIdAndDeletedFalse(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada con ID: " + prescriptionId));

        attention.setPrescription(prescription);
        prescription.setAttentionId(attention.getId());
        prescriptionRepository.save(prescription);
        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Receta ID: " + prescriptionId + " vinculada a Atención ID: " + attentionId
        );

        return attentionMapper.toDto(updated);
    }

    @Transactional
    public AttentionDto linkPayment(Long attentionId, Long paymentId) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(attentionId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + attentionId));

        Payment payment = paymentRepository.findByIdAndDeletedFalse(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + paymentId));

        attention.setPayment(payment);
        payment.setAttentionId(attention.getId());

        if (Payment.STATUS_PAGADO.equals(payment.getStatus())) {
            attention.setStatus(Attention.STATUS_COBRADA);
        }

        paymentRepository.save(payment);
        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Pago ID: " + paymentId + " vinculado a Atención ID: " + attentionId
        );

        return attentionMapper.toDto(updated);
    }
}
