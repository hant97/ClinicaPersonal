package com.clinica.backend.service;

import com.clinica.backend.dto.SpecialtyDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Specialty;
import com.clinica.backend.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private SpecialtyService specialtyService;

    private Specialty psychology;
    private Specialty dermatology;

    @BeforeEach
    void setUp() {
        psychology = new Specialty(1L, "PSICOLOGIA", "Psicología", "Atención en salud mental", "Brain", true, 1, null, null);
        dermatology = new Specialty(2L, "DERMATOLOGIA", "Dermatología", "Atención dermatológica", "Stethoscope", true, 2, null, null);
    }

    @Test
    @DisplayName("getActiveSpecialties should return only active specialties ordered by displayOrder")
    void getActiveSpecialties_ShouldReturnActiveSpecialties() {
        when(specialtyRepository.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc()).thenReturn(List.of(psychology, dermatology));

        List<SpecialtyDto> result = specialtyService.getActiveSpecialties();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("PSICOLOGIA", result.get(0).getCode());
        assertEquals("DERMATOLOGIA", result.get(1).getCode());
        verify(specialtyRepository, times(1)).findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
    }

    @Test
    @DisplayName("getAllSpecialties should return all specialties including inactive ones")
    void getAllSpecialties_ShouldReturnAllSpecialties() {
        Specialty inactive = new Specialty(3L, "NUTRICION", "Nutrición", "Atención nutricional", "Apple", false, 3, null, null);
        when(specialtyRepository.findAllByOrderByDisplayOrderAscCodeAsc()).thenReturn(List.of(psychology, dermatology, inactive));

        List<SpecialtyDto> result = specialtyService.getAllSpecialties();

        assertEquals(3, result.size());
        assertFalse(result.get(2).isActive());
        verify(specialtyRepository, times(1)).findAllByOrderByDisplayOrderAscCodeAsc();
    }

    @Test
    @DisplayName("getSpecialtyByCode should return DTO when specialty exists")
    void getSpecialtyByCode_WhenExists_ShouldReturnDto() {
        when(specialtyRepository.findByCode("PSICOLOGIA")).thenReturn(Optional.of(psychology));

        SpecialtyDto result = specialtyService.getSpecialtyByCode("psicologia");

        assertNotNull(result);
        assertEquals("PSICOLOGIA", result.getCode());
        assertEquals("Psicología", result.getName());
    }

    @Test
    @DisplayName("getSpecialtyByCode should throw ResourceNotFoundException when not found")
    void getSpecialtyByCode_WhenNotFound_ShouldThrow() {
        when(specialtyRepository.findByCode("CARDIOLOGIA")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> specialtyService.getSpecialtyByCode("CARDIOLOGIA"));
    }

    @Test
    @DisplayName("createSpecialty should save and return new specialty")
    void createSpecialty_WhenCodeIsUnique_ShouldSave() {
        SpecialtyDto newDto = new SpecialtyDto(null, "NUTRICION", "Nutrición", "Atención nutricional", "Apple", true, 3, null, null);
        Specialty saved = new Specialty(3L, "NUTRICION", "Nutrición", "Atención nutricional", "Apple", true, 3, null, null);

        when(specialtyRepository.existsByCode("NUTRICION")).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(saved);

        SpecialtyDto result = specialtyService.createSpecialty(newDto);

        assertNotNull(result);
        assertEquals("NUTRICION", result.getCode());
        assertEquals(3L, result.getId());
        verify(specialtyRepository, times(1)).save(any(Specialty.class));
    }

    @Test
    @DisplayName("createSpecialty should throw ConflictException when code already exists")
    void createSpecialty_WhenCodeExists_ShouldThrowConflict() {
        SpecialtyDto duplicateDto = new SpecialtyDto(null, "PSICOLOGIA", "Psicología Duplicada", null, "Brain", true, 1, null, null);
        when(specialtyRepository.existsByCode("PSICOLOGIA")).thenReturn(true);

        assertThrows(ConflictException.class, () -> specialtyService.createSpecialty(duplicateDto));
        verify(specialtyRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSpecialty should update fields and return modified specialty")
    void updateSpecialty_WhenFound_ShouldUpdate() {
        SpecialtyDto updateDto = new SpecialtyDto(null, "PSICOLOGIA", "Psicología Clínica y de la Salud", "Actualizado", "Brain", true, 1, null, null);
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(psychology));
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpecialtyDto result = specialtyService.updateSpecialty(1L, updateDto);

        assertNotNull(result);
        assertEquals("Psicología Clínica y de la Salud", result.getName());
        assertEquals("Actualizado", result.getDescription());
    }

    @Test
    @DisplayName("toggleActive should toggle the active state")
    void toggleActive_ShouldUpdateActiveField() {
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(psychology));
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpecialtyDto result = specialtyService.toggleActive(1L, false);

        assertNotNull(result);
        assertFalse(result.isActive());
    }
}
