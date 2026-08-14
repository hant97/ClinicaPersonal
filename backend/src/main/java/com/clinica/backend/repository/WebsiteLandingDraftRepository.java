package com.clinica.backend.repository;

import com.clinica.backend.model.WebsiteLandingDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebsiteLandingDraftRepository extends JpaRepository<WebsiteLandingDraft, Long> {
    Optional<WebsiteLandingDraft> findBySingletonKey(String singletonKey);
}
