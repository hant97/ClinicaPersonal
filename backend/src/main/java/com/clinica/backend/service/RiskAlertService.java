package com.clinica.backend.service;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.model.RiskAlert;
import com.clinica.backend.repository.RiskAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RiskAlertService {

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    public List<RiskAlertDto> getAlertsByPatientId(Long patientId, boolean onlyActive) {
        List<RiskAlert> alerts = onlyActive 
            ? riskAlertRepository.findByPatientIdAndActiveTrueOrderByCreatedAtDesc(patientId)
            : riskAlertRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        
        return alerts.stream().map(this::mapToDto).collect(Collectors.toList());
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
