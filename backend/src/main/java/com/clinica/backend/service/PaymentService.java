package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.dto.PatientBalanceDto;
import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.dto.PaymentItemDto;
import com.clinica.backend.dto.PaymentSummaryDto;
import com.clinica.backend.dto.PaymentTransactionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.*;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.AttentionRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.PaymentTransactionRepository;
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
import java.time.temporal.ChronoUnit;
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
    private static final int MAX_REPORT_RANGE_DAYS = 366;

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PatientRepository patientRepository;
    private final SupplyRepository supplyRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalSessionRepository clinicalSessionRepository;
    private final AttentionRepository attentionRepository;
    private final InventoryTransactionService inventoryTransactionService;
    private final AuditLogService auditLogService;

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    @Transactional(readOnly = true)
    public PaymentDto getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con id: " + id));
        if (!"TODAS".equals(getCurrentUserSpecialty()) && !payment.getSpecialty().equals(getCurrentUserSpecialty())) {
            throw new AccessDeniedException("No tiene permisos para acceder a este registro de otra especialidad");
        }
        return mapToDto(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getByPatientId(Long patientId, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        return paymentRepository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByPaymentDateDesc(patientId, specialty, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getAll(String searchTerm, LocalDate dateFrom, LocalDate dateTo, String paymentMethod, String status, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : LocalDate.of(1970, 1, 1).atStartOfDay();
        LocalDateTime to = dateTo != null ? dateTo.plusDays(1).atStartOfDay() : LocalDate.now().plusYears(100).atStartOfDay();
        return paymentRepository.findAllWithFiltersBySpecialty(searchTerm, paymentMethod, status, from, to, specialty, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PaymentSummaryDto getSummary(LocalDate dateFrom, LocalDate dateTo) {
        String specialty = getCurrentUserSpecialty();
        LocalDate today = LocalDate.now();

        // "Hoy" siempre refleja el día actual, independientemente del rango del reporte
        BigDecimal incomeToday = paymentTransactionRepository.sumIncomeBetweenBySpecialty(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), specialty);

        LocalDateTime rangeStart;
        LocalDateTime rangeEnd;
        LocalDate chartStart;
        long rangeDays;

        boolean customRange = dateFrom != null || dateTo != null;
        if (customRange) {
            LocalDate from = dateFrom != null ? dateFrom : LocalDate.of(1970, 1, 1);
            LocalDate to = dateTo != null ? dateTo : today;
            if (to.isBefore(from)) {
                throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
            }
            rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
            if (rangeDays > MAX_REPORT_RANGE_DAYS) {
                throw new IllegalArgumentException("El rango máximo del reporte es de " + MAX_REPORT_RANGE_DAYS + " días");
            }
            rangeStart = from.atStartOfDay();
            rangeEnd = to.plusDays(1).atStartOfDay();
            chartStart = from;
        } else {
            YearMonth currentMonth = YearMonth.from(today);
            rangeStart = currentMonth.atDay(1).atStartOfDay();
            rangeEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
            chartStart = today.minusDays(29);
            rangeDays = 30;
        }

        BigDecimal incomePeriod = paymentTransactionRepository.sumIncomeBetweenBySpecialty(rangeStart, rangeEnd, specialty);
        long paymentsCount = paymentTransactionRepository.countBetweenBySpecialty(rangeStart, rangeEnd, specialty);

        // Crecimiento: mes anterior (por defecto) o período equivalente inmediatamente anterior (rango personalizado)
        LocalDateTime previousStart;
        LocalDateTime previousEnd;
        if (customRange) {
            previousEnd = rangeStart;
            previousStart = rangeStart.minusDays(rangeDays);
        } else {
            previousEnd = rangeStart;
            previousStart = YearMonth.from(today).minusMonths(1).atDay(1).atStartOfDay();
        }
        BigDecimal incomePrevious = paymentTransactionRepository.sumIncomeBetweenBySpecialty(previousStart, previousEnd, specialty);

        int monthlyGrowth = 0;
        if (incomePrevious != null && incomePrevious.compareTo(BigDecimal.ZERO) > 0) {
            monthlyGrowth = incomePeriod.subtract(incomePrevious)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(incomePrevious, 0, RoundingMode.HALF_UP)
                    .intValue();
        } else if (incomePeriod != null && incomePeriod.compareTo(BigDecimal.ZERO) > 0) {
            monthlyGrowth = 100;
        }

        BigDecimal safeIncomePeriod = incomePeriod != null ? incomePeriod : BigDecimal.ZERO;
        BigDecimal averageTicket = paymentsCount > 0
                ? safeIncomePeriod.divide(BigDecimal.valueOf(paymentsCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<PaymentSummaryDto.MethodSummary> methodBreakdown = paymentTransactionRepository
                .sumByMethodBetweenBySpecialty(rangeStart, rangeEnd, specialty).stream()
                .map(row -> PaymentSummaryDto.MethodSummary.builder()
                        .method(row[0] != null ? row[0].toString() : "OTRO")
                        .total((BigDecimal) row[1])
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // Ingresos diarios del período graficado (los días sin cobros se devuelven con 0)
        LocalDateTime chartEnd = customRange ? rangeEnd : today.plusDays(1).atStartOfDay();
        Map<LocalDate, BigDecimal> incomeByDay = new HashMap<>();
        for (Object[] row : paymentTransactionRepository.sumDailyIncomeBetweenBySpecialty(chartStart.atStartOfDay(), chartEnd, specialty)) {
            incomeByDay.put(toLocalDate(row[0]), (BigDecimal) row[1]);
        }
        long chartDays = customRange ? rangeDays : 30;
        List<PaymentSummaryDto.DailyIncome> dailyIncome = new ArrayList<>();
        for (int i = 0; i < chartDays; i++) {
            LocalDate day = chartStart.plusDays(i);
            dailyIncome.add(PaymentSummaryDto.DailyIncome.builder()
                    .date(day)
                    .total(incomeByDay.getOrDefault(day, BigDecimal.ZERO))
                    .build());
        }

        List<PaymentSummaryDto.ServiceSummary> topServices = paymentRepository
                .findTopServicesBySpecialty(rangeStart, rangeEnd, specialty, PageRequest.of(0, 5)).stream()
                .map(row -> PaymentSummaryDto.ServiceSummary.builder()
                        .name(row[0].toString())
                        .quantity(((Number) row[1]).longValue())
                        .total((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        // Saldo pendiente global de la especialidad (cargos activos menos abonos activos)
        BigDecimal totalCharged = paymentRepository.sumChargedBySpecialty(specialty);
        BigDecimal totalReceived = paymentTransactionRepository.sumReceivedBySpecialty(specialty);
        BigDecimal pendingBalance = totalCharged.subtract(totalReceived).max(BigDecimal.ZERO);

        return PaymentSummaryDto.builder()
                .incomeToday(incomeToday != null ? incomeToday : BigDecimal.ZERO)
                .incomeMonth(safeIncomePeriod)
                .monthlyGrowth(monthlyGrowth)
                .paymentsCountMonth(paymentsCount)
                .averageTicket(averageTicket)
                .pendingBalance(pendingBalance)
                .methodBreakdown(methodBreakdown)
                .dailyIncome(dailyIncome)
                .topServices(topServices)
                .build();
    }

    @Transactional(readOnly = true)
    public PatientBalanceDto getPatientBalance(Long patientId) {
        String specialty = getCurrentUserSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, specialty)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        BigDecimal totalCharged = paymentRepository.sumChargedByPatientAndSpecialty(patient.getId(), specialty);
        BigDecimal totalPaid = paymentTransactionRepository.sumReceivedByPatientAndSpecialty(patient.getId(), specialty);

        return PatientBalanceDto.builder()
                .patientId(patient.getId())
                .totalCharged(totalCharged)
                .totalPaid(totalPaid)
                .balance(totalCharged.subtract(totalPaid).max(BigDecimal.ZERO))
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
        payment.setDueDate(dto.getDueDate());
        payment.setDescription(dto.getDescription());
        payment.setSpecialty(specialty);
        payment.setStatus(Payment.STATUS_PENDIENTE);
        payment.setAppointment(resolveAppointment(dto.getAppointmentId(), dto.getPatientId(), specialty));
        payment.setClinicalSession(resolveClinicalSession(dto.getClinicalSessionId(), dto.getPatientId(), specialty));
        payment.setAttentionId(dto.getAttentionId());
        payment.setItems(new ArrayList<>());
        payment.setTransactions(new ArrayList<>());

        Payment savedPayment = paymentRepository.save(payment);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PaymentItemDto itemDto : dto.getItems()) {
                savedPayment.getItems().add(buildItem(savedPayment, itemDto, specialty));
            }
        }

        if (dto.getTransactions() != null && !dto.getTransactions().isEmpty()) {
            for (PaymentTransactionDto transactionDto : dto.getTransactions()) {
                validateTransactionFits(savedPayment, transactionDto.getAmount());
                savedPayment.getTransactions().add(buildTransaction(savedPayment, transactionDto));
            }
        }
        recalculateStatus(savedPayment);

        Payment result = paymentRepository.save(savedPayment);

        if (dto.getAttentionId() != null) {
            attentionRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getAttentionId(), specialty)
                    .ifPresent(att -> {
                        att.setPayment(result);
                        if (Payment.STATUS_PAGADO.equals(result.getStatus())) {
                            att.setStatus(Attention.STATUS_COBRADA);
                        }
                        attentionRepository.save(att);
                    });
        }

        auditLogService.record(
                "CREATE",
                "PAYMENT",
                result.getId().toString(),
                "Cobro registrado por S/ " + result.getAmount() + " (" + result.getPaymentMethod() + ") para paciente ID: " + patient.getId()
        );

        return mapToDto(result);
    }

    @Transactional
    public PaymentDto update(Long id, PaymentDto dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobro no encontrado"));
        String specialty = getCurrentUserSpecialty();
        if (!specialty.equals(payment.getSpecialty())) {
            throw new AccessDeniedException("Cobro fuera de la especialidad del usuario");
        }
        if (dto.getPatientId() != null && !dto.getPatientId().equals(payment.getPatient().getId())) {
            throw new IllegalArgumentException("No se puede cambiar el paciente de un cobro");
        }

        validateAmount(dto.getAmount());
        validateItems(dto);

        BigDecimal paidAmount = paidAmount(payment);
        if (dto.getAmount().compareTo(paidAmount) < 0) {
            throw new IllegalArgumentException("El monto no puede ser menor al total ya abonado (S/ " + paidAmount + ")");
        }

        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setDueDate(dto.getDueDate());
        payment.setDescription(dto.getDescription());

        payment.setAppointment(dto.getAppointmentId() != null
                ? resolveAppointment(dto.getAppointmentId(), payment.getPatient().getId(), specialty)
                : null);
        payment.setClinicalSession(dto.getClinicalSessionId() != null
                ? resolveClinicalSession(dto.getClinicalSessionId(), payment.getPatient().getId(), specialty)
                : null);
        if (dto.getAttentionId() != null) {
            payment.setAttentionId(dto.getAttentionId());
        }

        // Revertir el stock consumido por los ítems anteriores y aplicar el de los nuevos
        restoreStockFromItems(payment, "AJUSTE_PAGO_" + payment.getId(),
                "Reversión de stock por edición de cobro #" + payment.getId());
        payment.getItems().clear();
        for (PaymentItemDto itemDto : dto.getItems()) {
            payment.getItems().add(buildItem(payment, itemDto, specialty));
        }

        recalculateStatus(payment);
        Payment updated = paymentRepository.save(payment);

        auditLogService.record(
                "UPDATE",
                "PAYMENT",
                updated.getId().toString(),
                "Cobro #" + updated.getId() + " actualizado con monto S/ " + updated.getAmount()
        );

        return mapToDto(updated);
    }

    @Transactional
    public PaymentDto addTransaction(Long paymentId, PaymentTransactionDto dto) {
        Payment payment = findActivePayment(paymentId);
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a 0");
        }
        validateTransactionFits(payment, dto.getAmount());
        payment.getTransactions().add(buildTransaction(payment, dto));
        recalculateStatus(payment);
        Payment saved = paymentRepository.save(payment);

        auditLogService.record(
                "CREATE",
                "PAYMENT_TRANSACTION",
                payment.getId().toString(),
                "Abono de S/ " + dto.getAmount() + " registrado al cobro #" + paymentId
        );

        return mapToDto(saved);
    }

    @Transactional
    public void deleteTransaction(Long paymentId, Long transactionId) {
        Payment payment = findActivePayment(paymentId);
        PaymentTransaction transaction = payment.getTransactions().stream()
                .filter(t -> !t.isDeleted() && t.getId() != null && t.getId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Abono no encontrado"));
        transaction.setDeleted(true);
        recalculateStatus(payment);
        paymentRepository.save(payment);

        auditLogService.record(
                "DELETE",
                "PAYMENT_TRANSACTION",
                transactionId.toString(),
                "Abono #" + transactionId + " eliminado del cobro #" + paymentId
        );
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

        auditLogService.record(
                "DELETE",
                "PAYMENT",
                id.toString(),
                "Cobro #" + id + " anulado/eliminado"
        );
    }

    private Payment findActivePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Cobro no encontrado"));
        if (!getCurrentUserSpecialty().equals(payment.getSpecialty())) {
            throw new AccessDeniedException("Cobro fuera de la especialidad del usuario");
        }
        if (payment.isDeleted()) {
            throw new IllegalArgumentException("Cobro no encontrado");
        }
        return payment;
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

    private ClinicalSession resolveClinicalSession(Long clinicalSessionId, Long patientId, String specialty) {
        if (clinicalSessionId == null) {
            return null;
        }
        ClinicalSession session = clinicalSessionRepository.findByIdAndDeletedFalse(clinicalSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión clínica no encontrada: " + clinicalSessionId));
        if (!specialty.equals(session.getSpecialty())) {
            throw new AccessDeniedException("Sesión clínica fuera de la especialidad del usuario");
        }
        if (session.getPatient() == null || !session.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("La sesión seleccionada no corresponde al paciente del cobro");
        }
        return session;
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

    private PaymentTransaction buildTransaction(Payment payment, PaymentTransactionDto dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a 0");
        }
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDateTime.now());
        transaction.setPaymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod() : payment.getPaymentMethod());
        transaction.setNotes(dto.getNotes());
        return transaction;
    }

    private void validateTransactionFits(Payment payment, BigDecimal newAmount) {
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a 0");
        }
        BigDecimal remaining = payment.getAmount().subtract(paidAmount(payment));
        if (newAmount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("El abono excede el saldo pendiente (S/ " + remaining + ")");
        }
    }

    private BigDecimal paidAmount(Payment payment) {
        if (payment.getTransactions() == null) {
            return BigDecimal.ZERO;
        }
        return payment.getTransactions().stream()
                .filter(t -> !t.isDeleted() && t.getAmount() != null)
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recalculateStatus(Payment payment) {
        BigDecimal paid = paidAmount(payment);
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            payment.setStatus(Payment.STATUS_PENDIENTE);
        } else if (paid.compareTo(payment.getAmount()) < 0) {
            payment.setStatus(Payment.STATUS_PARCIAL);
        } else {
            payment.setStatus(Payment.STATUS_PAGADO);
        }
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
        dto.setStatus(payment.getStatus());
        dto.setDueDate(payment.getDueDate());
        if (payment.getPatient() != null) {
            dto.setPatientName(payment.getPatient().getFullName());
        }
        if (payment.getAppointment() != null) {
            dto.setAppointmentId(payment.getAppointment().getId());
        }
        if (payment.getClinicalSession() != null) {
            dto.setClinicalSessionId(payment.getClinicalSession().getId());
        }
        dto.setAttentionId(payment.getAttentionId());

        BigDecimal paid = paidAmount(payment);
        dto.setPaidAmount(paid);
        dto.setBalanceAmount(payment.getAmount().subtract(paid).max(BigDecimal.ZERO));

        if (payment.getItems() != null && !payment.getItems().isEmpty()) {
            dto.setItems(payment.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        } else {
            dto.setItems(new ArrayList<>());
        }

        if (payment.getTransactions() != null && !payment.getTransactions().isEmpty()) {
            dto.setTransactions(payment.getTransactions().stream()
                    .filter(t -> !t.isDeleted())
                    .map(this::mapTransactionToDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setTransactions(new ArrayList<>());
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

    private PaymentTransactionDto mapTransactionToDto(PaymentTransaction transaction) {
        PaymentTransactionDto dto = new PaymentTransactionDto();
        dto.setId(transaction.getId());
        dto.setPaymentId(transaction.getPayment().getId());
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setPaymentMethod(transaction.getPaymentMethod());
        dto.setNotes(transaction.getNotes());
        return dto;
    }
}
