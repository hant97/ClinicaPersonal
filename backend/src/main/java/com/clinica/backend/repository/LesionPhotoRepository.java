package com.clinica.backend.repository;

import com.clinica.backend.model.LesionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LesionPhotoRepository extends JpaRepository<LesionPhoto, Long> {
    List<LesionPhoto> findByLesionIdOrderByCreatedAtDesc(Long lesionId);
}
