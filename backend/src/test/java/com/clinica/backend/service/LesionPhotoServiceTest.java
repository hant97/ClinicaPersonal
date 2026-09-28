package com.clinica.backend.service;

import com.clinica.backend.mapper.LesionPhotoMapperImpl;
import com.clinica.backend.model.Lesion;
import com.clinica.backend.model.LesionPhoto;
import com.clinica.backend.repository.LesionPhotoRepository;
import com.clinica.backend.repository.LesionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LesionPhotoServiceTest {

    @Mock
    private LesionPhotoRepository repository;
    @Mock
    private LesionRepository lesionRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @Mock
    private ClinicalFileStorage fileStorage;
    @Spy
    private LesionPhotoMapperImpl lesionPhotoMapper = new LesionPhotoMapperImpl();
    @InjectMocks
    private LesionPhotoService service;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadPhotoDeletesStoredFileWhenTransactionRollsBack() {
        Lesion lesion = lesion(4L);
        MockMultipartFile file = new MockMultipartFile("file", "lesion.png", "image/png", new byte[]{1, 2, 3});
        when(lesionRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(lesion));
        when(fileStorage.store(any(), eq("lesions"))).thenReturn("lesions/abc.png");
        when(repository.save(any(LesionPhoto.class))).thenAnswer(invocation -> {
            LesionPhoto photo = invocation.getArgument(0);
            photo.setId(8L);
            return photo;
        });
        TransactionSynchronizationManager.initSynchronization();

        var result = service.uploadPhoto(4L, file, "control", null);

        assertEquals("lesions/abc.png", result.getFileUrl());
        verify(fileStorage, never()).delete(any());
        completeSynchronization(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorage).delete("lesions/abc.png");
    }

    @Test
    void deletePhotoRemovesStoredFileOnlyAfterCommit() {
        LesionPhoto photo = new LesionPhoto();
        photo.setId(8L);
        photo.setLesion(lesion(4L));
        photo.setFileUrl("lesions/abc.png");
        when(repository.findById(8L)).thenReturn(Optional.of(photo));
        TransactionSynchronizationManager.initSynchronization();

        service.deletePhoto(8L);

        verify(repository).delete(photo);
        verify(fileStorage, never()).delete(any());
        completeSynchronization(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileStorage).delete("lesions/abc.png");
    }

    @Test
    void deletePhotoKeepsStoredFileWhenTransactionRollsBack() {
        LesionPhoto photo = new LesionPhoto();
        photo.setId(8L);
        photo.setLesion(lesion(4L));
        photo.setFileUrl("lesions/abc.png");
        when(repository.findById(8L)).thenReturn(Optional.of(photo));
        TransactionSynchronizationManager.initSynchronization();

        service.deletePhoto(8L);
        completeSynchronization(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorage, never()).delete(any());
    }

    private static Lesion lesion(Long id) {
        Lesion lesion = new Lesion();
        lesion.setId(id);
        lesion.setProfessionalId(12L);
        return lesion;
    }

    private void completeSynchronization(int status) {
        try {
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(status));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
