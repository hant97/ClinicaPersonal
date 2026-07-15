package com.clinica.backend.service;
import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Payment;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    public List<PaymentDto> getByPatientId(Long patientId) {
        return paymentRepository.findByPatientIdOrderByPaymentDateDesc(patientId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }
    public List<PaymentDto> getAll() {
        return paymentRepository.findAllByOrderByPaymentDateDesc().stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }
    public PaymentDto create(PaymentDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId()).orElseThrow();
        Payment payment = new Payment();
        payment.setPatient(patient);
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDescription(dto.getDescription());
        return mapToDto(paymentRepository.save(payment));
    }
    private PaymentDto mapToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setPatientId(payment.getPatient().getId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setDescription(payment.getDescription());
        return dto;
    }
}
