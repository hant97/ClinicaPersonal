package com.clinica.backend.controller;

import com.clinica.backend.dto.PatientBalanceDto;
import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.dto.PaymentSummaryDto;
import com.clinica.backend.dto.PaymentTransactionDto;
import com.clinica.backend.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService)).build();
    }

    @Test
    void getAllPassesFiltersToService() throws Exception {
        Page<PaymentDto> empty = new PageImpl<>(List.of(), Pageable.ofSize(20), 0);
        when(paymentService.getAll(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/api/v1/payments")
                        .param("searchTerm", "ana")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-01-31")
                        .param("paymentMethod", "EFECTIVO")
                        .param("status", "PARCIAL")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> methodCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getAll(searchCaptor.capture(), fromCaptor.capture(), toCaptor.capture(), methodCaptor.capture(), statusCaptor.capture(), pageableCaptor.capture());

        assertThat(searchCaptor.getValue()).isEqualTo("ana");
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(methodCaptor.getValue()).isEqualTo("EFECTIVO");
        assertThat(statusCaptor.getValue()).isEqualTo("PARCIAL");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getSummaryReturnsOk() throws Exception {
        when(paymentService.getSummary(any(), any())).thenReturn(PaymentSummaryDto.builder().build());

        mockMvc.perform(get("/api/v1/payments/summary"))
                .andExpect(status().isOk());
    }

    @Test
    void getSummaryPassesDateRangeToService() throws Exception {
        when(paymentService.getSummary(any(), any())).thenReturn(PaymentSummaryDto.builder().build());

        mockMvc.perform(get("/api/v1/payments/summary")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-01-31"))
                .andExpect(status().isOk());

        verify(paymentService).getSummary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    }

    @Test
    void getPatientBalanceReturnsOk() throws Exception {
        when(paymentService.getPatientBalance(5L)).thenReturn(
                PatientBalanceDto.builder().patientId(5L)
                        .totalCharged(new BigDecimal("100.00"))
                        .totalPaid(new BigDecimal("40.00"))
                        .balance(new BigDecimal("60.00"))
                        .build());

        mockMvc.perform(get("/api/v1/payments/patient/5/balance"))
                .andExpect(status().isOk());

        verify(paymentService).getPatientBalance(5L);
    }

    @Test
    void addTransactionReturnsUpdatedPayment() throws Exception {
        PaymentDto dto = new PaymentDto();
        dto.setId(10L);
        when(paymentService.addTransaction(eq(10L), any(PaymentTransactionDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/payments/10/transactions")
                        .contentType("application/json")
                        .content("{\"amount\": 25.00, \"paymentMethod\": \"EFECTIVO\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<PaymentTransactionDto> captor = ArgumentCaptor.forClass(PaymentTransactionDto.class);
        verify(paymentService).addTransaction(eq(10L), captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void deleteTransactionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/payments/10/transactions/3"))
                .andExpect(status().isNoContent());

        verify(paymentService).deleteTransaction(10L, 3L);
    }
}
