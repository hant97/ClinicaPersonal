package com.clinica.backend.service;

import com.clinica.backend.mapper.LesionPhotoMapper;

import com.clinica.backend.dto.LesionPhotoDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Lesion;
import com.clinica.backend.model.LesionPhoto;
import com.clinica.backend.repository.LesionPhotoRepository;
import com.clinica.backend.repository.LesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LesionPhotoService {

    private final LesionPhotoRepository repository;
    private final LesionRepository lesionRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final ClinicalFileStorage fileStorage;
    private final LesionPhotoMapper lesionPhotoMapper;

    @Transactional(readOnly = true)
    public List<LesionPhotoDto> getPhotos(Long lesionId) {
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Lesion lesion = getActiveLesion(lesionId);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", lesion.getProfessionalId());
        return repository.findByLesionIdOrderByCreatedAtDesc(lesionId).stream()
                .map(lesionPhotoMapper::toDto)
                .toList();
    }

    @Transactional
    public LesionPhotoDto uploadPhoto(Long lesionId, MultipartFile file, String description, LocalDate takenDate) {
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Lesion lesion = getActiveLesion(lesionId);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", lesion.getProfessionalId());

        String key = fileStorage.store(file, "lesions");
        LesionPhoto photo = new LesionPhoto();
        photo.setLesion(lesion);
        photo.setFileUrl(key);
        photo.setDescription(description);
        photo.setTakenDate(takenDate);
        return lesionPhotoMapper.toDto(repository.save(photo));
    }

    @Transactional
    public void deletePhoto(Long photoId) {
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        LesionPhoto photo = repository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Fotografía no encontrada"));
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", photo.getLesion().getProfessionalId());
        fileStorage.delete(photo.getFileUrl());
        repository.delete(photo);
    }

    @Transactional(readOnly = true)
    public Resource loadPhoto(Long photoId) {
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        LesionPhoto photo = repository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Fotografía no encontrada"));
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", photo.getLesion().getProfessionalId());
        return fileStorage.load(photo.getFileUrl());
    }

    private Lesion getActiveLesion(Long id) {
        return lesionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesión no encontrada"));
    }

}
