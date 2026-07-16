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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    
    public List<PaymentDto> getByPatientId(Long patientId) {
        return paymentRepository.findByPatientIdAndDeletedFalseOrderByPaymentDateDesc(patientId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }
    
    public Page<PaymentDto> getAll(Pageable pageable) {
        return paymentRepository.findAllByDeletedFalseOrderByPaymentDateDesc(pageable).map(this::mapToDto);
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

    public PaymentDto update(Long id, PaymentDto dto) {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDescription(dto.getDescription());
        return mapToDto(paymentRepository.save(payment));
    }

    public void delete(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        payment.setDeleted(true);
        paymentRepository.save(payment);
    }
}
