package com.clinica.backend.service;

import com.clinica.backend.dto.AttentionSummaryDto;
import com.clinica.backend.dto.ProfessionalProductivityDto;
import com.clinica.backend.model.Attention;
import com.clinica.backend.model.Payment;
import com.clinica.backend.model.PaymentTransaction;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AttentionRepository;
import com.clinica.backend.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttentionReportService {
    private final AttentionRepository attentionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public AttentionSummaryDto getTodaySummary() {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();
        LocalDate today = LocalDate.now();

        List<Attention> todayAttentions = attentionRepository.findByAttentionDateAndSpecialtyAndDeletedFalse(today, specialty);

        long total = todayAttentions.size();
        long scheduled = 0;
        long inProgress = 0;
        long attended = 0;
        long paid = 0;
        long cancelled = 0;
        BigDecimal pendingBilling = BigDecimal.ZERO;

        for (Attention a : todayAttentions) {
            switch (a.getStatus()) {
                case Attention.STATUS_AGENDADA -> scheduled++;
                case Attention.STATUS_EN_PROCESO -> inProgress++;
                case Attention.STATUS_ATENDIDA, Attention.STATUS_COBRADA -> {
                    Payment payment = a.getPayment();
                    if (payment != null && !payment.isDeleted()
                            && Payment.STATUS_PAGADO.equals(payment.getStatus())) {
                        paid++;
                    } else if (payment != null && !payment.isDeleted()) {
                        attended++;
                        BigDecimal paidAmount = payment.getTransactions().stream()
                                .filter(t -> !t.isDeleted() && t.getAmount() != null)
                                .map(PaymentTransaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        pendingBilling = pendingBilling.add(payment.getAmount().subtract(paidAmount).max(BigDecimal.ZERO));
                    } else {
                        attended++;
                        if (a.getClinicalService() != null && a.getClinicalService().getPrice() != null) {
                            pendingBilling = pendingBilling.add(a.getClinicalService().getPrice());
                        }
                    }
                }
                case Attention.STATUS_CANCELADA -> cancelled++;
            }
        }

        return new AttentionSummaryDto(total, scheduled, inProgress, attended, paid, cancelled, pendingBilling);
    }

    /**
     * Reporte gerencial de productividad por profesional dentro de la especialidad del
     * administrador autenticado. Agrupa las atenciones del período por profesional,
     * calculando volumen y tasa de finalización por fecha de atención, monto facturado
     * desde el cobro vinculado y abonos recibidos por fecha de transacción.
     * Solo debe exponerse a usuarios con rol ADMIN (restricción aplicada en el controlador).
     */
    @Transactional(readOnly = true)
    public List<ProfessionalProductivityDto> getProfessionalProductivity(LocalDate dateFrom, LocalDate dateTo) {
        User admin = clinicalAuthorizationService.currentUser();
        String specialty = admin.getSpecialty();

        LocalDate today = LocalDate.now();
        LocalDate from = dateFrom != null ? dateFrom : today.withDayOfMonth(1);
        LocalDate to = dateTo != null ? dateTo : today;
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }

        List<Attention> attentions = attentionRepository
                .findBySpecialtyAndAttentionDateBetweenAndDeletedFalse(specialty, from, to);

        Map<Long, List<Attention>> byProfessional = attentions.stream()
                .filter(a -> a.getProfessional() != null)
                .collect(Collectors.groupingBy(a -> a.getProfessional().getId()));

        Map<Long, ProfessionalProductivityDto> byProfessionalId = new HashMap<>();
        for (Map.Entry<Long, List<Attention>> entry : byProfessional.entrySet()) {
            List<Attention> list = entry.getValue();
            User professional = list.get(0).getProfessional();

            long total = list.size();
            long attended = list.stream().filter(a -> Attention.STATUS_ATENDIDA.equals(a.getStatus()) || Attention.STATUS_COBRADA.equals(a.getStatus())).count();
            long cancelled = list.stream().filter(a -> Attention.STATUS_CANCELADA.equals(a.getStatus())).count();
            long paid = list.stream().filter(a -> (Attention.STATUS_ATENDIDA.equals(a.getStatus())
                    || Attention.STATUS_COBRADA.equals(a.getStatus()))
                    && a.getPayment() != null
                    && !a.getPayment().isDeleted()
                    && Payment.STATUS_PAGADO.equals(a.getPayment().getStatus())).count();

            BigDecimal billed = list.stream()
                    .filter(a -> Attention.STATUS_ATENDIDA.equals(a.getStatus()) || Attention.STATUS_COBRADA.equals(a.getStatus()))
                    .map(a -> a.getPayment() != null && !a.getPayment().isDeleted() && a.getPayment().getAmount() != null
                            ? a.getPayment().getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long completionBase = total - cancelled;
            int completionRate = completionBase > 0 ? (int) Math.round((attended * 100.0) / completionBase) : 0;

            byProfessionalId.put(entry.getKey(), ProfessionalProductivityDto.builder()
                    .professionalId(entry.getKey())
                    .professionalName(professional.getFullName())
                    .totalAttentions(total)
                    .attendedAttentions(attended)
                    .cancelledAttentions(cancelled)
                    .paidAttentions(paid)
                    .completionRate(completionRate)
                    .billedAmount(billed)
                    .collectedAmount(BigDecimal.ZERO)
                    .build());
        }

        for (Object[] row : paymentTransactionRepository.sumReceivedByProfessionalBetween(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), specialty)) {
            Long professionalId = ((Number) row[0]).longValue();
            String firstName = row[1] != null ? row[1].toString().trim() : "";
            String lastName = row[2] != null ? row[2].toString().trim() : "";
            String fullName = (firstName + " " + lastName).trim();
            ProfessionalProductivityDto productivity = byProfessionalId.computeIfAbsent(professionalId,
                    id -> ProfessionalProductivityDto.builder()
                            .professionalId(id)
                            .professionalName(fullName.isEmpty() ? row[3].toString() : fullName)
                            .billedAmount(BigDecimal.ZERO)
                            .collectedAmount(BigDecimal.ZERO)
                            .build());
            productivity.setCollectedAmount((BigDecimal) row[4]);
        }

        List<ProfessionalProductivityDto> result = new ArrayList<>(byProfessionalId.values());
        result.sort((a, b) -> b.getBilledAmount().compareTo(a.getBilledAmount()));
        return result;
    }
}
