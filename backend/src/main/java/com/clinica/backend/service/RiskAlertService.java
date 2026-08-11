package com.clinica.backend.service;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.model.RiskAlert;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiskAlertService {

    private final RiskAlertRepository riskAlertRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public Page<RiskAlertDto> getAlertsByPatientId(Long patientId, boolean onlyActive, Pageable pageable) {
        Page<RiskAlert> alerts = onlyActive
                ? riskAlertRepository.findByPatientIdAndActiveTrueOrderByCreatedAtDesc(patientId, pageable)
                : riskAlertRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);

        return alerts.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<RiskAlertDto> getAllActiveAlerts(Pageable pageable) {
        Page<RiskAlert> alerts = riskAlertRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
        return alerts.map(this::mapToDto);
    }

    @Transactional
    public RiskAlertDto createAlert(RiskAlertDto dto) {
        if (dto.getPatientId() == null) {
            throw new IllegalArgumentException("El ID de paciente es obligatorio");
        }
        if (!patientRepository.existsByIdAndDeletedFalse(dto.getPatientId())) {
            throw new IllegalArgumentException("El paciente no existe o está dado de baja: " + dto.getPatientId());
        }

        RiskAlert alert = new RiskAlert();
        alert.setPatientId(dto.getPatientId());
        alert.setType(dto.getType());
        alert.setLevel(dto.getLevel());
        alert.setDescription(dto.getDescription());
        alert.setActive(true);

        RiskAlert saved = riskAlertRepository.save(alert);
        return mapToDto(saved);
    }

    @Transactional
    public RiskAlertDto resolveAlert(Long id) {
        RiskAlert alert = riskAlertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta de riesgo no encontrada con ID: " + id));
        alert.setActive(false);
        alert.setResolvedAt(LocalDateTime.now());

        RiskAlert saved = riskAlertRepository.save(alert);
        return mapToDto(saved);
    }

    private RiskAlertDto mapToDto(RiskAlert entity) {
        RiskAlertDto dto = new RiskAlertDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setType(entity.getType());
        dto.setLevel(entity.getLevel());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.isActive());
        dto.setResolvedAt(entity.getResolvedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
