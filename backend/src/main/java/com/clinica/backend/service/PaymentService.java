package com.clinica.backend.service;

import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Payment;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.model.PaymentItem;
import com.clinica.backend.dto.PaymentItemDto;
import com.clinica.backend.model.Supply;
import com.clinica.backend.repository.SupplyRepository;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.model.TransactionType;
import com.clinica.backend.model.TransactionReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final SupplyRepository supplyRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final InventoryTransactionService inventoryTransactionService;

    public Page<PaymentDto> getByPatientId(Long patientId, Pageable pageable) {
        return paymentRepository.findByPatientIdAndDeletedFalseOrderByPaymentDateDesc(patientId, pageable)
                .map(this::mapToDto);
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
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDescription(dto.getDescription());
        
        // Validation: At least one ClinicalService must be present
        boolean hasClinicalService = false;
        if (dto.getItems() != null) {
            for (PaymentItemDto itemDto : dto.getItems()) {
                if (itemDto.getClinicalServiceId() != null) {
                    hasClinicalService = true;
                    break;
                }
            }
        }
        
        if (!hasClinicalService) {
            throw new RuntimeException("A payment must contain at least one Clinical Service.");
        }
        
        Payment savedPayment = paymentRepository.save(payment);
        
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PaymentItemDto itemDto : dto.getItems()) {
                PaymentItem item = new PaymentItem();
                item.setPayment(savedPayment);
                item.setDescription(itemDto.getDescription());
                item.setQuantity(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setTotalPrice(itemDto.getTotalPrice());
                
                if (itemDto.getClinicalServiceId() != null) {
                    ClinicalService service = clinicalServiceRepository.findById(itemDto.getClinicalServiceId()).orElseThrow();
                    item.setClinicalService(service);
                }
                
                if (itemDto.getSupplyId() != null) {
                    Supply supply = supplyRepository.findById(itemDto.getSupplyId()).orElseThrow();
                    item.setSupply(supply);
                    
                    // Generate inventory transaction
                    InventoryTransactionDto txDto = new InventoryTransactionDto();
                    txDto.setSupplyId(supply.getId());
                    txDto.setQuantity(-itemDto.getQuantity()); // Salida
                    txDto.setType(TransactionType.OUT);
                    txDto.setReason(TransactionReason.BILLING);
                    txDto.setReferenceId(savedPayment.getId().toString());
                    txDto.setNotes("Cobro de factura");
                    inventoryTransactionService.recordTransaction(txDto);
                }
                
                savedPayment.getItems().add(item);
            }
            paymentRepository.save(savedPayment);
        }
        
        return mapToDto(savedPayment);
    }

    private PaymentDto mapToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setPatientId(payment.getPatient().getId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setDescription(payment.getDescription());
        
        if (payment.getItems() != null && !payment.getItems().isEmpty()) {
            dto.setItems(payment.getItems().stream().map(this::mapItemToDto).collect(java.util.stream.Collectors.toList()));
        } else {
            dto.setItems(new java.util.ArrayList<>());
        }
        
        return dto;
    }
    
    private PaymentItemDto mapItemToDto(PaymentItem item) {
        PaymentItemDto dto = new PaymentItemDto();
        dto.setId(item.getId());
        dto.setPaymentId(item.getPayment().getId());
        dto.setDescription(item.getDescription());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        if (item.getSupply() != null) {
            dto.setSupplyId(item.getSupply().getId());
        }
        if (item.getClinicalService() != null) {
            dto.setClinicalServiceId(item.getClinicalService().getId());
        }
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
