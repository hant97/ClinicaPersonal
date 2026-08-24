package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.dto.PatientBalanceDto;
import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.dto.PaymentItemDto;
import com.clinica.backend.dto.PaymentSummaryDto;
import com.clinica.backend.dto.PaymentTransactionDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Payment;
import com.clinica.backend.model.PaymentItem;
import com.clinica.backend.model.PaymentTransaction;
import com.clinica.backend.model.Supply;
import com.clinica.backend.model.TransactionReason;
import com.clinica.backend.model.TransactionType;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.PaymentTransactionRepository;
import com.clinica.backend.repository.SupplyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentTransactionRepository paymentTransactionRepository;
    private PatientRepository patientRepository;
    private SupplyRepository supplyRepository;
    private ClinicalServiceRepository clinicalServiceRepository;
    private AppointmentRepository appointmentRepository;
    private ClinicalSessionRepository clinicalSessionRepository;
    private com.clinica.backend.repository.AttentionRepository attentionRepository;
    private InventoryTransactionService inventoryTransactionService;
    private AuditLogService auditLogService;
    private PaymentService paymentService;
    private PaymentReportService paymentReportService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentTransactionRepository = mock(PaymentTransactionRepository.class);
        patientRepository = mock(PatientRepository.class);
        supplyRepository = mock(SupplyRepository.class);
        clinicalServiceRepository = mock(ClinicalServiceRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        clinicalSessionRepository = mock(ClinicalSessionRepository.class);
        attentionRepository = mock(com.clinica.backend.repository.AttentionRepository.class);
        inventoryTransactionService = mock(InventoryTransactionService.class);
        auditLogService = mock(AuditLogService.class);
        paymentService = new PaymentService(paymentRepository, patientRepository, supplyRepository,
                clinicalServiceRepository, appointmentRepository, clinicalSessionRepository,
                attentionRepository, inventoryTransactionService, auditLogService);
        paymentReportService = new PaymentReportService(paymentRepository, paymentTransactionRepository, patientRepository);

        User user = new User();
        user.setId(1L);
        user.setUsername("doctor");
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_ADMIN"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private PaymentItemDto serviceItem(Long clinicalServiceId, int quantity, String unitPrice) {
        PaymentItemDto item = new PaymentItemDto();
        item.setDescription("Consulta psicológica");
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setClinicalServiceId(clinicalServiceId);
        return item;
    }

    private PaymentItemDto supplyItem(Long supplyId, int quantity, String unitPrice) {
        PaymentItemDto item = new PaymentItemDto();
        item.setDescription("Insumo descartable");
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setSupplyId(supplyId);
        return item;
    }

    private PaymentTransactionDto transactionDto(String amount) {
        PaymentTransactionDto dto = new PaymentTransactionDto();
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }

    private Patient mockPatient(Long id) {
        Patient patient = new Patient();
        patient.setId(id);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        return patient;
    }

    private ClinicalService mockClinicalService(Long id) {
        ClinicalService service = new ClinicalService();
        service.setId(id);
        when(clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(id, "PSICOLOGIA"))
                .thenReturn(Optional.of(service));
        return service;
    }

    private void mockSaveAssignsId() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
    }

    private Payment existingPayment(String amount, String paidAmount) {
        Patient patient = new Patient();
        patient.setId(5L);

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setPatient(patient);
        payment.setSpecialty("PSICOLOGIA");
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setItems(new ArrayList<>());
        payment.setTransactions(new ArrayList<>());
        payment.setStatus(Payment.STATUS_PENDIENTE);

        if (new BigDecimal(paidAmount).compareTo(BigDecimal.ZERO) > 0) {
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setId(1L);
            transaction.setPayment(payment);
            transaction.setAmount(new BigDecimal(paidAmount));
            transaction.setTransactionDate(LocalDateTime.now());
            payment.getTransactions().add(transaction);
        }

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return payment;
    }

    @Test
    void createShouldFailWhenNoClinicalServiceItemPresent() {
        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(supplyItem(9L, 1, "50.00")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.create(dto));
        assertTrue(ex.getMessage().contains("al menos un servicio clínico"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createShouldFailWhenItemsTotalDoesNotMatchAmount() {
        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("999.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.create(dto));
        assertTrue(ex.getMessage().contains("no coincide"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createShouldSucceedAndConsumeStockForSupplies() {
        mockPatient(5L);
        mockClinicalService(3L);

        Supply supply = new Supply();
        supply.setId(9L);
        when(supplyRepository.findByIdAndSpecialtyAndDeletedFalse(9L, "PSICOLOGIA"))
                .thenReturn(Optional.of(supply));

        mockSaveAssignsId();

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("70.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setPaymentMethod("EFECTIVO");
        dto.setItems(List.of(serviceItem(3L, 1, "50.00"), supplyItem(9L, 2, "10.00")));

        PaymentDto created = paymentService.create(dto);

        assertNotNull(created);
        assertEquals(100L, created.getId());
        assertEquals(2, created.getItems().size());

        ArgumentCaptor<InventoryTransactionDto> txCaptor = ArgumentCaptor.forClass(InventoryTransactionDto.class);
        verify(inventoryTransactionService).recordTransaction(txCaptor.capture());
        InventoryTransactionDto tx = txCaptor.getValue();
        assertEquals(9L, tx.getSupplyId());
        assertEquals(2, tx.getQuantity());
        assertEquals(TransactionType.OUT, tx.getType());
        assertEquals(TransactionReason.BILLING, tx.getReason());
        assertEquals("COBRO_100", tx.getReferenceId());
    }

    @Test
    void createShouldStartAsPendingWithoutTransactions() {
        mockPatient(5L);
        mockClinicalService(3L);
        mockSaveAssignsId();

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        PaymentDto created = paymentService.create(dto);

        assertEquals(Payment.STATUS_PENDIENTE, created.getStatus());
        assertEquals(0, created.getPaidAmount().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("50.00"), created.getBalanceAmount());
        assertTrue(created.getTransactions().isEmpty());
    }

    @Test
    void createShouldMarkAsPaidWhenInitialTransactionCoversAmount() {
        mockPatient(5L);
        mockClinicalService(3L);
        mockSaveAssignsId();

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setPaymentMethod("EFECTIVO");
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));
        dto.setTransactions(List.of(transactionDto("50.00")));

        PaymentDto created = paymentService.create(dto);

        assertEquals(Payment.STATUS_PAGADO, created.getStatus());
        assertEquals(new BigDecimal("50.00"), created.getPaidAmount());
        assertEquals(0, created.getBalanceAmount().compareTo(BigDecimal.ZERO));
        assertEquals(1, created.getTransactions().size());
        // El abono hereda el método del cobro cuando no se indica otro
        assertEquals("EFECTIVO", created.getTransactions().get(0).getPaymentMethod());
    }

    @Test
    void createShouldMarkAsPartialWhenInitialTransactionIsLower() {
        mockPatient(5L);
        mockClinicalService(3L);
        mockSaveAssignsId();

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));
        dto.setTransactions(List.of(transactionDto("20.00")));

        PaymentDto created = paymentService.create(dto);

        assertEquals(Payment.STATUS_PARCIAL, created.getStatus());
        assertEquals(new BigDecimal("30.00"), created.getBalanceAmount());
    }

    @Test
    void createShouldFailWhenInitialTransactionsExceedAmount() {
        mockPatient(5L);
        mockClinicalService(3L);
        mockSaveAssignsId();

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));
        dto.setTransactions(List.of(transactionDto("30.00"), transactionDto("30.00")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.create(dto));
        assertTrue(ex.getMessage().contains("excede el saldo pendiente"));
    }

    @Test
    void createShouldLinkAppointmentWhenValid() {
        Patient patient = mockPatient(5L);
        mockClinicalService(3L);

        Appointment appointment = new Appointment();
        appointment.setId(77L);
        appointment.setSpecialty("PSICOLOGIA");
        appointment.setPatient(patient);
        when(appointmentRepository.findById(77L)).thenReturn(Optional.of(appointment));

        mockSaveAssignsId();

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setAppointmentId(77L);
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        PaymentDto created = paymentService.create(dto);

        assertEquals(77L, created.getAppointmentId());
    }

    @Test
    void createShouldFailWhenAppointmentBelongsToAnotherPatient() {
        mockPatient(5L);

        Patient otherPatient = new Patient();
        otherPatient.setId(99L);

        Appointment appointment = new Appointment();
        appointment.setId(77L);
        appointment.setSpecialty("PSICOLOGIA");
        appointment.setPatient(otherPatient);
        when(appointmentRepository.findById(77L)).thenReturn(Optional.of(appointment));

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setAppointmentId(77L);
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.create(dto));
        assertTrue(ex.getMessage().contains("no corresponde al paciente"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updateShouldReplaceItemsAndAdjustStock() {
        Patient patient = new Patient();
        patient.setId(5L);

        Supply oldSupply = new Supply();
        oldSupply.setId(9L);

        PaymentItem oldItem = new PaymentItem();
        oldItem.setQuantity(2);
        oldItem.setSupply(oldSupply);

        Payment existing = new Payment();
        existing.setId(100L);
        existing.setPatient(patient);
        existing.setSpecialty("PSICOLOGIA");
        existing.setAmount(new BigDecimal("70.00"));
        existing.setItems(new ArrayList<>(List.of(oldItem)));
        existing.setTransactions(new ArrayList<>());
        oldItem.setPayment(existing);

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockClinicalService(3L);

        Supply newSupply = new Supply();
        newSupply.setId(10L);
        when(supplyRepository.findByIdAndSpecialtyAndDeletedFalse(10L, "PSICOLOGIA"))
                .thenReturn(Optional.of(newSupply));

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("65.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setPaymentMethod("YAPE");
        dto.setItems(List.of(serviceItem(3L, 1, "50.00"), supplyItem(10L, 3, "5.00")));

        PaymentDto updated = paymentService.update(100L, dto);

        assertEquals(new BigDecimal("65.00"), updated.getAmount());
        assertEquals(2, updated.getItems().size());

        // Debe revertir el stock del ítem anterior y consumir el del nuevo
        ArgumentCaptor<InventoryTransactionDto> txCaptor = ArgumentCaptor.forClass(InventoryTransactionDto.class);
        verify(inventoryTransactionService, times(2)).recordTransaction(txCaptor.capture());
        List<InventoryTransactionDto> txs = txCaptor.getAllValues();

        InventoryTransactionDto restoreTx = txs.stream()
                .filter(t -> t.getType() == TransactionType.IN).findFirst().orElseThrow();
        assertEquals(9L, restoreTx.getSupplyId());
        assertEquals(2, restoreTx.getQuantity());
        assertEquals("AJUSTE_PAGO_100", restoreTx.getReferenceId());

        InventoryTransactionDto outTx = txs.stream()
                .filter(t -> t.getType() == TransactionType.OUT).findFirst().orElseThrow();
        assertEquals(10L, outTx.getSupplyId());
        assertEquals(3, outTx.getQuantity());
        assertEquals("COBRO_100", outTx.getReferenceId());
    }

    @Test
    void updateShouldFailWhenItemsTotalDoesNotMatchAmount() {
        Payment existing = new Payment();
        existing.setId(100L);
        existing.setPatient(new Patient());
        existing.setSpecialty("PSICOLOGIA");
        existing.setItems(new ArrayList<>());
        existing.setTransactions(new ArrayList<>());

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(existing));

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("999.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        assertThrows(IllegalArgumentException.class, () -> paymentService.update(100L, dto));
        verify(paymentRepository, never()).save(any());
        verify(inventoryTransactionService, never()).recordTransaction(any());
    }

    @Test
    void updateShouldFailWhenAmountBelowAlreadyPaid() {
        existingPayment("100.00", "60.00");

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.update(100L, dto));
        assertTrue(ex.getMessage().contains("ya abonado"));
        verify(paymentRepository, never()).save(any());
        verify(inventoryTransactionService, never()).recordTransaction(any());
    }

    @Test
    void updateShouldRejectChangingPatient() {
        Patient patient = new Patient();
        patient.setId(5L);

        Payment existing = new Payment();
        existing.setId(100L);
        existing.setPatient(patient);
        existing.setSpecialty("PSICOLOGIA");
        existing.setItems(new ArrayList<>());
        existing.setTransactions(new ArrayList<>());

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(existing));

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(99L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.update(100L, dto));
        assertTrue(ex.getMessage().contains("No se puede cambiar el paciente"));
        verify(paymentRepository, never()).save(any());
        verify(inventoryTransactionService, never()).recordTransaction(any());
    }

    @Test
    void updateShouldRequireItems() {
        Patient patient = new Patient();
        patient.setId(5L);

        Payment existing = new Payment();
        existing.setId(100L);
        existing.setPatient(patient);
        existing.setSpecialty("PSICOLOGIA");
        existing.setItems(new ArrayList<>());
        existing.setTransactions(new ArrayList<>());

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(existing));

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setItems(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.update(100L, dto));
        assertTrue(ex.getMessage().contains("al menos un servicio clínico"));
        verify(paymentRepository, never()).save(any());
        verify(inventoryTransactionService, never()).recordTransaction(any());
    }

    @Test
    void updateShouldUnlinkAppointmentWhenNull() {
        Patient patient = new Patient();
        patient.setId(5L);

        Appointment appointment = new Appointment();
        appointment.setId(77L);
        appointment.setSpecialty("PSICOLOGIA");
        appointment.setPatient(patient);

        Payment existing = new Payment();
        existing.setId(100L);
        existing.setPatient(patient);
        existing.setSpecialty("PSICOLOGIA");
        existing.setAppointment(appointment);
        existing.setItems(new ArrayList<>());
        existing.setTransactions(new ArrayList<>());

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockClinicalService(3L);

        PaymentDto dto = new PaymentDto();
        dto.setPatientId(5L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setPaymentDate(LocalDateTime.now());
        dto.setAppointmentId(null);
        dto.setItems(List.of(serviceItem(3L, 1, "50.00")));

        PaymentDto updated = paymentService.update(100L, dto);

        assertNull(updated.getAppointmentId());
        assertNull(existing.getAppointment());
    }

    @Test
    void addTransactionShouldMoveFromPendingToPartial() {
        existingPayment("100.00", "0");

        PaymentDto result = paymentService.addTransaction(100L, transactionDto("40.00"));

        assertEquals(Payment.STATUS_PARCIAL, result.getStatus());
        assertEquals(new BigDecimal("40.00"), result.getPaidAmount());
        assertEquals(new BigDecimal("60.00"), result.getBalanceAmount());
        assertEquals(1, result.getTransactions().size());
    }

    @Test
    void addTransactionShouldMoveFromPartialToPaid() {
        existingPayment("100.00", "40.00");

        PaymentDto result = paymentService.addTransaction(100L, transactionDto("60.00"));

        assertEquals(Payment.STATUS_PAGADO, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getPaidAmount());
        assertEquals(0, result.getBalanceAmount().compareTo(BigDecimal.ZERO));
        assertEquals(2, result.getTransactions().size());
    }

    @Test
    void addTransactionShouldFailWhenExceedsBalance() {
        existingPayment("100.00", "40.00");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.addTransaction(100L, transactionDto("70.00")));
        assertTrue(ex.getMessage().contains("excede el saldo pendiente"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void addTransactionShouldFailWhenAmountIsInvalid() {
        existingPayment("100.00", "0");

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.addTransaction(100L, transactionDto("0")));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void addTransactionShouldFailWhenPaymentIsDeleted() {
        Payment payment = existingPayment("100.00", "0");
        payment.setDeleted(true);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.addTransaction(100L, transactionDto("10.00")));
    }

    @Test
    void addTransactionShouldFailWhenPaymentFromOtherSpecialty() {
        Payment payment = existingPayment("100.00", "0");
        payment.setSpecialty("DERMATOLOGIA");

        assertThrows(AccessDeniedException.class,
                () -> paymentService.addTransaction(100L, transactionDto("10.00")));
    }

    @Test
    void deleteTransactionShouldRecalculateStatusToPending() {
        Payment payment = existingPayment("100.00", "100.00");
        payment.setStatus(Payment.STATUS_PAGADO);

        paymentService.deleteTransaction(100L, 1L);

        assertTrue(payment.getTransactions().get(0).isDeleted());
        assertEquals(Payment.STATUS_PENDIENTE, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void deleteTransactionShouldFailWhenTransactionNotFound() {
        existingPayment("100.00", "0");

        assertThrows(IllegalArgumentException.class, () -> paymentService.deleteTransaction(100L, 999L));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getPatientBalanceShouldReturnTotals() {
        mockPatient(5L);
        when(paymentRepository.sumChargedByPatientAndSpecialty(5L, "PSICOLOGIA"))
                .thenReturn(new BigDecimal("500.00"));
        when(paymentTransactionRepository.sumReceivedByPatientAndSpecialty(5L, "PSICOLOGIA"))
                .thenReturn(new BigDecimal("200.00"));

        PatientBalanceDto balance = paymentReportService.getPatientBalance(5L);

        assertEquals(5L, balance.getPatientId());
        assertEquals(new BigDecimal("500.00"), balance.getTotalCharged());
        assertEquals(new BigDecimal("200.00"), balance.getTotalPaid());
        assertEquals(new BigDecimal("300.00"), balance.getBalance());
    }

    @Test
    void getPatientBalanceShouldFailWhenPatientNotFound() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(5L, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> paymentReportService.getPatientBalance(5L));
    }

    @Test
    void deleteShouldRestoreStock() {
        Supply supply = new Supply();
        supply.setId(9L);

        PaymentItem item = new PaymentItem();
        item.setQuantity(2);
        item.setSupply(supply);

        Payment existing = new Payment();
        existing.setId(100L);
        existing.setPatient(new Patient());
        existing.setSpecialty("PSICOLOGIA");
        existing.setItems(new ArrayList<>(List.of(item)));
        item.setPayment(existing);

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.delete(100L);

        assertTrue(existing.isDeleted());

        ArgumentCaptor<InventoryTransactionDto> txCaptor = ArgumentCaptor.forClass(InventoryTransactionDto.class);
        verify(inventoryTransactionService).recordTransaction(txCaptor.capture());
        InventoryTransactionDto tx = txCaptor.getValue();
        assertEquals(TransactionType.IN, tx.getType());
        assertEquals(TransactionReason.RESTOCK, tx.getReason());
        assertEquals("ANULACION_PAGO_100", tx.getReferenceId());
        assertEquals(2, tx.getQuantity());
    }

    @Test
    void getSummaryShouldCalculateKpis() {
        when(paymentTransactionRepository.sumIncomeBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(new BigDecimal("50.00"), new BigDecimal("1500.00"), new BigDecimal("1000.00"));
        when(paymentTransactionRepository.countBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(10L);
        when(paymentTransactionRepository.sumByMethodBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.<Object[]>of(new Object[]{"EFECTIVO", new BigDecimal("900.00"), 6L}));
        when(paymentTransactionRepository.sumDailyIncomeBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.now(), new BigDecimal("50.00")}));
        when(paymentRepository.findTopServicesBySpecialty(any(), any(), eq("PSICOLOGIA"), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[]{"Consulta Psicológica", 8L, new BigDecimal("1200.00")}));
        when(paymentRepository.sumChargedBySpecialty("PSICOLOGIA")).thenReturn(new BigDecimal("2000.00"));
        when(paymentTransactionRepository.sumReceivedBySpecialty("PSICOLOGIA")).thenReturn(new BigDecimal("1500.00"));

        PaymentSummaryDto summary = paymentReportService.getSummary(null, null);

        assertEquals(new BigDecimal("50.00"), summary.getIncomeToday());
        assertEquals(new BigDecimal("1500.00"), summary.getIncomeMonth());
        assertEquals(50, summary.getMonthlyGrowth());
        assertEquals(10L, summary.getPaymentsCountMonth());
        assertEquals(new BigDecimal("150.00"), summary.getAverageTicket());
        assertEquals(new BigDecimal("500.00"), summary.getPendingBalance());

        assertEquals(1, summary.getMethodBreakdown().size());
        assertEquals("EFECTIVO", summary.getMethodBreakdown().get(0).getMethod());

        assertEquals(30, summary.getDailyIncome().size());
        assertEquals(new BigDecimal("50.00"), summary.getDailyIncome().get(29).getTotal());
        assertEquals(BigDecimal.ZERO, summary.getDailyIncome().get(0).getTotal());

        assertEquals(1, summary.getTopServices().size());
        assertEquals("Consulta Psicológica", summary.getTopServices().get(0).getName());
        assertEquals(8L, summary.getTopServices().get(0).getQuantity());
    }

    @Test
    void getSummaryWithCustomRangeUsesRangeBounds() {
        when(paymentTransactionRepository.sumIncomeBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(new BigDecimal("300.00"));
        when(paymentTransactionRepository.countBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(3L);
        when(paymentTransactionRepository.sumByMethodBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.of());
        when(paymentTransactionRepository.sumDailyIncomeBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.of());
        when(paymentRepository.findTopServicesBySpecialty(any(), any(), eq("PSICOLOGIA"), any(Pageable.class)))
                .thenReturn(List.of());
        when(paymentRepository.sumChargedBySpecialty("PSICOLOGIA")).thenReturn(new BigDecimal("1000.00"));
        when(paymentTransactionRepository.sumReceivedBySpecialty("PSICOLOGIA")).thenReturn(new BigDecimal("400.00"));

        PaymentSummaryDto summary = paymentReportService.getSummary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));

        assertEquals(new BigDecimal("300.00"), summary.getIncomeMonth());
        assertEquals(3L, summary.getPaymentsCountMonth());
        assertEquals(new BigDecimal("100.00"), summary.getAverageTicket());
        assertEquals(new BigDecimal("600.00"), summary.getPendingBalance());
        assertEquals(10, summary.getDailyIncome().size());
        assertEquals(LocalDate.of(2026, 1, 1), summary.getDailyIncome().get(0).getDate());

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(paymentTransactionRepository, atLeastOnce())
                .sumIncomeBetweenBySpecialty(startCaptor.capture(), endCaptor.capture(), eq("PSICOLOGIA"));
        assertTrue(startCaptor.getAllValues().contains(LocalDate.of(2026, 1, 1).atStartOfDay()));
        assertTrue(endCaptor.getAllValues().contains(LocalDate.of(2026, 1, 11).atStartOfDay()));
    }

    @Test
    void getSummaryShouldFailWhenRangeIsInverted() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentReportService.getSummary(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void getSummaryShouldFailWhenRangeExceedsMaxDays() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentReportService.getSummary(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 2, 1)));
    }
}
