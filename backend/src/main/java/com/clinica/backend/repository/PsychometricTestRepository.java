package com.clinica.backend.repository;

import com.clinica.backend.model.PsychometricTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PsychometricTestRepository extends JpaRepository<PsychometricTest, Long> {
}
