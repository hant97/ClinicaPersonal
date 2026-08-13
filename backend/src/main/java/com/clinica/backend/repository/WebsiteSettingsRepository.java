package com.clinica.backend.repository;

import com.clinica.backend.model.WebsiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebsiteSettingsRepository extends JpaRepository<WebsiteSettings, Long> {
    Optional<WebsiteSettings> findBySingletonKey(String singletonKey);
}
