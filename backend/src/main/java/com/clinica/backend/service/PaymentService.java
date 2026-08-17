package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.dto.PaymentItemDto;
import com.clinica.backend.dto.PaymentSummaryDto;
import com.clinica.backend.model.*;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.SupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final SupplyRepository supplyRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final InventoryTransactionService inventoryTransactionService;

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getByPatientId(Long patientId, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        return paymentRepository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByPaymentDateDesc(patientId, specialty, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getAll(String searchTerm, LocalDate dateFrom, LocalDate dateTo, String paymentMethod, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : LocalDate.of(1970, 1, 1).atStartOfDay();
        LocalDateTime to = dateTo != null ? dateTo.plusDays(1).atStartOfDay() : LocalDate.now().plusYears(100).atStartOfDay();
        return paymentRepository.findAllWithFiltersBySpecialty(searchTerm, paymentMethod, from, to, specialty, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PaymentSummaryDto getSummary() {
        String specialty = getCurrentUserSpecialty();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime startOfNextMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime startOfPreviousMonth = currentMonth.minusMonths(1).atDay(1).atStartOfDay();

        BigDecimal incomeToday = paymentRepository.sumIncomeBetweenBySpecialty(today.atStartOfDay(), today.plusDays(1).atStartOfDay(), specialty);
        BigDecimal incomeMonth = paymentRepository.sumIncomeBetweenBySpecialty(startOfMonth, startOfNextMonth, specialty);
        BigDecimal incomePreviousMonth = paymentRepository.sumIncomeBetweenBySpecialty(startOfPreviousMonth, startOfMonth, specialty);
        long paymentsCountMonth = paymentRepository.countBetweenBySpecialty(startOfMonth, startOfNextMonth, specialty);

        int monthlyGrowth = 0;
        if (incomePreviousMonth != null && incomePreviousMonth.compareTo(BigDecimal.ZERO) > 0) {
            monthlyGrowth = incomeMonth.subtract(incomePreviousMonth)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(incomePreviousMonth, 0, RoundingMode.HALF_UP)
                    .intValue();
        } else if (incomeMonth != null && incomeMonth.compareTo(BigDecimal.ZERO) > 0) {
            monthlyGrowth = 100;
        }

        BigDecimal safeIncomeMonth = incomeMonth != null ? incomeMonth : BigDecimal.ZERO;
        BigDecimal averageTicket = paymentsCountMonth > 0
                ? safeIncomeMonth.divide(BigDecimal.valueOf(paymentsCountMonth), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<PaymentSummaryDto.MethodSummary> methodBreakdown = paymentRepository
                .sumByMethodBetweenBySpecialty(startOfMonth, startOfNextMonth, specialty).stream()
                .map(row -> PaymentSummaryDto.MethodSummary.builder()
                        .method(row[0] != null ? row[0].toString() : "OTRO")
                        .total((BigDecimal) row[1])
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // Ingresos diarios de los últimos 30 días (los días sin cobros se devuelven con 0)
        LocalDate chartStart = today.minusDays(29);
        Map<LocalDate, BigDecimal> incomeByDay = new HashMap<>();
        for (Object[] row : paymentRepository.sumDailyIncomeBetweenBySpecialty(chartStart.atStartOfDay(), today.plusDays(1).atStartOfDay(), specialty)) {
            incomeByDay.put(toLocalDate(row[0]), (BigDecimal) row[1]);
        }
        List<PaymentSummaryDto.DailyIncome> dailyIncome = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate day = chartStart.plusDays(i);
            dailyIncome.add(PaymentSummaryDto.DailyIncome.builder()
                    .date(day)
                    .total(incomeByDay.getOrDefault(day, BigDecimal.ZERO))
                    .build());
        }

        List<PaymentSummaryDto.ServiceSummary> topServices = paymentRepository
                .findTopServicesBySpecialty(startOfMonth, startOfNextMonth, specialty, PageRequest.of(0, 5)).stream()
                .map(row -> PaymentSummaryDto.ServiceSummary.builder()
                        .name(row[0].toString())
                        .quantity(((Number) row[1]).longValue())
                        .total((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        return PaymentSummaryDto.builder()
                .incomeToday(incomeToday != null ? incomeToday : BigDecimal.ZERO)
                .incomeMonth(safeIncomeMonth)
                .monthlyGrowth(monthlyGrowth)
                .paymentsCountMonth(paymentsCountMonth)
                .averageTicket(averageTicket)
                .methodBreakdown(methodBreakdown)
                .dailyIncome(dailyIncome)
                .topServices(topServices)
                .build();
    }

    @Transactional
    public PaymentDto create(PaymentDto dto) {
        if (dto.getPatientId() == null) {
            throw new IllegalArgumentException("El paciente es obligatorio");
        }
        validateAmount(dto.getAmount());
        validateItems(dto);

        String specialty = getCurrentUserSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), specialty)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        Payment payment = new Payment();
        payment.setPatient(patient);
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDescription(dto.getDescription());
        payment.setSpecialty(specialty);
        payment.setAppointment(resolveAppointment(dto.getAppointmentId(), dto.getPatientId(), specialty));
        payment.setItems(new ArrayList<>());

        Payment savedPayment = paymentRepository.save(payment);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PaymentItemDto itemDto : dto.getItems()) {
                savedPayment.getItems().add(buildItem(savedPayment, itemDto, specialty));
            }
            paymentRepository.save(savedPayment);
        }

        return mapToDto(savedPayment);
    }

    @Transactional
    public PaymentDto update(Long id, PaymentDto dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobro no encontrado"));
        String specialty = getCurrentUserSpecialty();
        if (!specialty.equals(payment.getSpecialty())) {
            throw new AccessDeniedException("Cobro fuera de la especialidad del usuario");
        }

        validateAmount(dto.getAmount());
        if (dto.getItems() != null) {
            validateItems(dto);
        }

        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDescription(dto.getDescription());

        if (dto.getAppointmentId() != null) {
            payment.setAppointment(resolveAppointment(dto.getAppointmentId(), payment.getPatient().getId(), specialty));
        }

        if (dto.getItems() != null) {
            // Revertir el stock consumido por los ítems anteriores y aplicar el de los nuevos
            restoreStockFromItems(payment, "AJUSTE_PAGO_" + payment.getId(),
                    "Reversión de stock por edición de cobro #" + payment.getId());
            payment.getItems().clear();
            for (PaymentItemDto itemDto : dto.getItems()) {
                payment.getItems().add(buildItem(payment, itemDto, specialty));
            }
        }

        return mapToDto(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobro no encontrado"));
        if (!getCurrentUserSpecialty().equals(payment.getSpecialty())) {
            throw new AccessDeniedException("Cobro fuera de la especialidad del usuario");
        }
        if (payment.isDeleted()) {
            return;
        }
        payment.setDeleted(true);
        paymentRepository.save(payment);

        restoreStockFromItems(payment, "ANULACION_PAGO_" + payment.getId(),
                "Reversión de stock por eliminación de cobro #" + payment.getId());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del cobro debe ser mayor a 0");
        }
    }

    private void validateItems(PaymentDto dto) {
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
    }

    private Appointment resolveAppointment(Long appointmentId, Long patientId, String specialty) {
        if (appointmentId == null) {
            return null;
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada: " + appointmentId));
        if (!specialty.equals(appointment.getSpecialty())) {
            throw new AccessDeniedException("Cita fuera de la especialidad del usuario");
        }
        if (appointment.getPatient() == null || !appointment.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("La cita seleccionada no corresponde al paciente del cobro");
        }
        return appointment;
    }

    private PaymentItem buildItem(Payment payment, PaymentItemDto itemDto, String specialty) {
        PaymentItem item = new PaymentItem();
        item.setPayment(payment);
        item.setDescription(itemDto.getDescription());
        item.setQuantity(itemDto.getQuantity());
        item.setUnitPrice(itemDto.getUnitPrice());
        item.setTotalPrice(itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));

        if (itemDto.getClinicalServiceId() != null) {
            ClinicalService service = clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(itemDto.getClinicalServiceId(), specialty)
                    .orElseThrow(() -> new IllegalArgumentException("Servicio clínico no encontrado: " + itemDto.getClinicalServiceId()));
            item.setClinicalService(service);
        }

        if (itemDto.getSupplyId() != null) {
            Supply supply = supplyRepository.findByIdAndSpecialtyAndDeletedFalse(itemDto.getSupplyId(), specialty)
                    .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado: " + itemDto.getSupplyId()));
            item.setSupply(supply);

            // Generate inventory transaction
            InventoryTransactionDto txDto = new InventoryTransactionDto();
            txDto.setSupplyId(supply.getId());
            txDto.setQuantity(itemDto.getQuantity());
            txDto.setType(TransactionType.OUT);
            txDto.setReason(TransactionReason.BILLING);
            txDto.setReferenceId("COBRO_" + payment.getId());
            txDto.setNotes("Salida por cobro/factura #" + payment.getId());
            inventoryTransactionService.recordTransaction(txDto);
        }

        return item;
    }

    private void restoreStockFromItems(Payment payment, String referenceId, String notes) {
        if (payment.getItems() == null) {
            return;
        }
        for (PaymentItem item : payment.getItems()) {
            if (item.getSupply() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                InventoryTransactionDto restoreTx = new InventoryTransactionDto();
                restoreTx.setSupplyId(item.getSupply().getId());
                restoreTx.setQuantity(item.getQuantity());
                restoreTx.setType(TransactionType.IN);
                restoreTx.setReason(TransactionReason.RESTOCK);
                restoreTx.setReferenceId(referenceId);
                restoreTx.setNotes(notes);
                inventoryTransactionService.recordTransaction(restoreTx);
            }
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    private PaymentDto mapToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setPatientId(payment.getPatient().getId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setDescription(payment.getDescription());
        dto.setSpecialty(payment.getSpecialty());
        if (payment.getAppointment() != null) {
            dto.setAppointmentId(payment.getAppointment().getId());
        }

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
