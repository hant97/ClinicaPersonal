package com.clinica.backend.service;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.model.RiskAlert;
import com.clinica.backend.repository.RiskAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class RiskAlertService {

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    public Page<RiskAlertDto> getAlertsByPatientId(Long patientId, boolean onlyActive, Pageable pageable) {
        Page<RiskAlert> alerts = onlyActive
                ? riskAlertRepository.findByPatientIdAndActiveTrueOrderByCreatedAtDesc(patientId, pageable)
                : riskAlertRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);

        return alerts.map(this::mapToDto);
    }

    public Page<RiskAlertDto> getAllActiveAlerts(Pageable pageable) {
        Page<RiskAlert> alerts = riskAlertRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
        return alerts.map(this::mapToDto);
    }

    public RiskAlertDto createAlert(RiskAlertDto dto) {
        RiskAlert alert = new RiskAlert();
        alert.setPatientId(dto.getPatientId());
        alert.setType(dto.getType());
        alert.setLevel(dto.getLevel());
        alert.setDescription(dto.getDescription());
        alert.setActive(true);

        RiskAlert saved = riskAlertRepository.save(alert);
        return mapToDto(saved);
    }

    public RiskAlertDto resolveAlert(Long id) {
        RiskAlert alert = riskAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
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
