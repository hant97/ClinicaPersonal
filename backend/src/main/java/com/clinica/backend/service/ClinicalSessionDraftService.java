package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalSessionDraftRequest;
import com.clinica.backend.dto.ClinicalSessionDraftResponse;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.ClinicalSessionDraft;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalSessionDraftRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClinicalSessionDraftService {

    private static final int MAX_CONTENT_BYTES = 64 * 1024;
    private static final int EXPIRATION_DAYS = 7;

    private final ClinicalSessionDraftRepository draftRepository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<ClinicalSessionDraftResponse> getDraft(Long patientId) {
        User professional = clinicalAuthorizationService.currentProfessional();
        getPatient(patientId, professional);

        Optional<ClinicalSessionDraft> draft = draftRepository
                .findByProfessionalIdAndPatientId(professional.getId(), patientId);
        if (draft.isEmpty()) {
            return Optional.empty();
        }

        ClinicalSessionDraft savedDraft = draft.get();
        if (!savedDraft.getExpiresAt().isAfter(LocalDateTime.now())) {
            return Optional.empty();
        }

        return Optional.of(toResponse(savedDraft));
    }

    @Transactional
    public ClinicalSessionDraftResponse saveDraft(Long patientId, ClinicalSessionDraftRequest request) {
        User professional = clinicalAuthorizationService.currentProfessional();
        Patient patient = getPatient(patientId, professional);
        JsonNode content = request.getContent();
        if (content == null || !content.isObject()) {
            throw new IllegalArgumentException("El contenido del borrador debe ser un objeto JSON");
        }

        String contentJson = objectMapper.writeValueAsString(content);
        if (contentJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException("El borrador supera el tamaño máximo permitido");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<ClinicalSessionDraft> existing = draftRepository
                .findByProfessionalIdAndPatientId(professional.getId(), patientId);
        if (existing.isPresent() && !existing.get().getVersion().equals(request.getVersion())) {
            throw new ConflictException("El borrador cambió en otra pestaña. Revise la versión guardada antes de continuar.");
        }
        if (existing.isEmpty() && request.getVersion() != null) {
            throw new ConflictException("El borrador ya no existe. Revise el estado actual antes de continuar.");
        }
        ClinicalSessionDraft draft = existing.orElseGet(ClinicalSessionDraft::new);
        draft.setProfessional(professional);
        draft.setPatient(patient);
        draft.setContentJson(contentJson);
        draft.setExpiresAt(now.plusDays(EXPIRATION_DAYS));
        return toResponse(draftRepository.saveAndFlush(draft));
    }

    @Transactional
    public void deleteDraft(Long patientId, Long version) {
        User professional = clinicalAuthorizationService.currentProfessional();
        getPatient(patientId, professional);
        Optional<ClinicalSessionDraft> existing = draftRepository
                .findByProfessionalIdAndPatientId(professional.getId(), patientId);
        if (existing.isEmpty()) {
            return;
        }
        if (!existing.get().getVersion().equals(version)) {
            throw new ConflictException("El borrador cambió en otra pestaña y no se puede eliminar.");
        }
        draftRepository.delete(existing.get());
        draftRepository.flush();
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void deleteExpiredDrafts() {
        draftRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private Patient getPatient(Long patientId, User professional) {
        return patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, professional.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
    }

    private ClinicalSessionDraftResponse toResponse(ClinicalSessionDraft draft) {
        return new ClinicalSessionDraftResponse(
                objectMapper.readTree(draft.getContentJson()),
                draft.getExpiresAt(),
                draft.getVersion());
    }
}
