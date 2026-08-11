package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.dto.PaymentItemDto;
import com.clinica.backend.model.*;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.SupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final SupplyRepository supplyRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final InventoryTransactionService inventoryTransactionService;

    @Transactional(readOnly = true)
    public Page<PaymentDto> getByPatientId(Long patientId, Pageable pageable) {
        return paymentRepository.findByPatientIdAndDeletedFalseOrderByPaymentDateDesc(patientId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getAll(String searchTerm, Pageable pageable) {
        return paymentRepository.findAllWithSearch(searchTerm, pageable).map(this::mapToDto);
    }

    @Transactional
    public PaymentDto create(PaymentDto dto) {
        if (dto.getPatientId() == null) {
            throw new IllegalArgumentException("El paciente es obligatorio");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del cobro debe ser mayor a 0");
        }

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        // Validation: At least one ClinicalService must be present and items totals must match
        boolean hasClinicalService = false;
        BigDecimal calculatedTotal = BigDecimal.ZERO;

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PaymentItemDto itemDto : dto.getItems()) {
                if (itemDto.getClinicalServiceId() != null) {
                    hasClinicalService = true;
                }
                if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                    throw new IllegalArgumentException("La cantidad de cada ítem debe ser mayor a 0");
                }
                if (itemDto.getUnitPrice() == null || itemDto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("El precio unitario no puede ser negativo");
                }
                BigDecimal itemTotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                calculatedTotal = calculatedTotal.add(itemTotal);
            }
        }

        if (!hasClinicalService) {
            throw new IllegalArgumentException("Un cobro debe incluir al menos un servicio clínico.");
        }

        if (calculatedTotal.compareTo(BigDecimal.ZERO) > 0 && calculatedTotal.compareTo(dto.getAmount()) != 0) {
            throw new IllegalArgumentException("El monto total del cobro (" + dto.getAmount() + ") no coincide con la suma calculada de los ítems (" + calculatedTotal + ").");
        }

        Payment payment = new Payment();
        payment.setPatient(patient);
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDescription(dto.getDescription());
        payment.setItems(new ArrayList<>());

        Payment savedPayment = paymentRepository.save(payment);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PaymentItemDto itemDto : dto.getItems()) {
                PaymentItem item = new PaymentItem();
                item.setPayment(savedPayment);
                item.setDescription(itemDto.getDescription());
                item.setQuantity(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setTotalPrice(itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));

                if (itemDto.getClinicalServiceId() != null) {
                    ClinicalService service = clinicalServiceRepository.findById(itemDto.getClinicalServiceId())
                            .orElseThrow(() -> new IllegalArgumentException("Servicio clínico no encontrado: " + itemDto.getClinicalServiceId()));
                    item.setClinicalService(service);
                }

                if (itemDto.getSupplyId() != null) {
                    Supply supply = supplyRepository.findById(itemDto.getSupplyId())
                            .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado: " + itemDto.getSupplyId()));
                    item.setSupply(supply);

                    // Generate inventory transaction
                    InventoryTransactionDto txDto = new InventoryTransactionDto();
                    txDto.setSupplyId(supply.getId());
                    txDto.setQuantity(itemDto.getQuantity());
                    txDto.setType(TransactionType.OUT);
                    txDto.setReason(TransactionReason.BILLING);
                    txDto.setReferenceId("COBRO_" + savedPayment.getId());
                    txDto.setNotes("Salida por cobro/factura #" + savedPayment.getId());
                    inventoryTransactionService.recordTransaction(txDto);
                }

                savedPayment.getItems().add(item);
            }
            paymentRepository.save(savedPayment);
        }

        return mapToDto(savedPayment);
    }

    @Transactional
    public PaymentDto update(Long id, PaymentDto dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobro no encontrado"));
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDescription(dto.getDescription());
        return mapToDto(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobro no encontrado"));
        if (payment.isDeleted()) {
            return;
        }
        payment.setDeleted(true);
        paymentRepository.save(payment);

        // Compensate / restore stock for consumed supplies
        if (payment.getItems() != null) {
            for (PaymentItem item : payment.getItems()) {
                if (item.getSupply() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    InventoryTransactionDto restoreTx = new InventoryTransactionDto();
                    restoreTx.setSupplyId(item.getSupply().getId());
                    restoreTx.setQuantity(item.getQuantity());
                    restoreTx.setType(TransactionType.IN);
                    restoreTx.setReason(TransactionReason.RESTOCK);
                    restoreTx.setReferenceId("ANULACION_PAGO_" + payment.getId());
                    restoreTx.setNotes("Reversión de stock por eliminación de cobro #" + payment.getId());
                    inventoryTransactionService.recordTransaction(restoreTx);
                }
            }
        }
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
            dto.setItems(payment.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        } else {
            dto.setItems(new ArrayList<>());
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
}
