package com.clinica.backend.service.provider;

import com.clinica.backend.dto.ClinicalHistoryDto;
import org.springframework.data.domain.Pageable;

/**
 * Strategy interface for modular clinical history section providers.
 * Enables new specialties to contribute their specific clinical history sections
 * without modifying the core ClinicalHistoryService aggregator.
 */
public interface ClinicalHistorySectionProvider {

    /**
     * Returns the specialty code this provider handles (e.g., "PSICOLOGIA", "DERMATOLOGIA").
     */
    String getSupportedSpecialty();

    /**
     * Populates specialty-specific sections into the aggregated clinical history DTO.
     *
     * @param patientId ID of the patient
     * @param dto Aggregate DTO to populate
     * @param pageable Paging configuration
     */
    void populateSections(Long patientId, ClinicalHistoryDto dto, Pageable pageable);
}
