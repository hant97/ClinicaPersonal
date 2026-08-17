package com.clinica.backend.controller;

import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.dto.PaymentSummaryDto;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(paymentService.getAll(any(), any(), any(), any(), any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/api/v1/payments")
                        .param("searchTerm", "ana")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-01-31")
                        .param("paymentMethod", "EFECTIVO")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> methodCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getAll(searchCaptor.capture(), fromCaptor.capture(), toCaptor.capture(), methodCaptor.capture(), pageableCaptor.capture());

        assertThat(searchCaptor.getValue()).isEqualTo("ana");
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(methodCaptor.getValue()).isEqualTo("EFECTIVO");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getSummaryReturnsOk() throws Exception {
        when(paymentService.getSummary()).thenReturn(PaymentSummaryDto.builder().build());

        mockMvc.perform(get("/api/v1/payments/summary"))
                .andExpect(status().isOk());
    }
}
