package com.clinica.backend.service;

import com.clinica.backend.dto.ScheduleBlockDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ScheduleBlock;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ScheduleBlockRepository;
import com.clinica.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleBlockService {

    private final ScheduleBlockRepository blockRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getCurrentUserSpecialty() {
        return getCurrentUser().getSpecialty();
    }

    @Transactional(readOnly = true)
    public List<ScheduleBlockDto> getBlocks(LocalDate startDate, LocalDate endDate, Long professionalId) {
        String specialty = getCurrentUserSpecialty();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusMonths(6);

        List<ScheduleBlock> blocks = blockRepository.findBlocksInRange(specialty, professionalId, start, end);
        return blocks.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ScheduleBlockDto createBlock(ScheduleBlockDto dto) {
        validateBlockDates(dto);
        String specialty = getCurrentUserSpecialty();

        ScheduleBlock block = new ScheduleBlock();
        block.setProfessionalId(dto.getProfessionalId());
        block.setSpecialty(specialty);
        block.setTitle(dto.getTitle().trim());
        block.setStartDate(dto.getStartDate());
        block.setEndDate(dto.getEndDate());
        block.setStartTime(dto.getStartTime());
        block.setEndTime(dto.getEndTime());
        block.setReason(dto.getReason() != null ? dto.getReason().trim() : null);

        ScheduleBlock saved = blockRepository.save(block);
        return mapToDto(saved);
    }

    @Transactional
    public ScheduleBlockDto updateBlock(Long id, ScheduleBlockDto dto) {
        validateBlockDates(dto);
        String specialty = getCurrentUserSpecialty();

        ScheduleBlock block = blockRepository.findByIdAndSpecialty(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo de agenda no encontrado"));

        block.setProfessionalId(dto.getProfessionalId());
        block.setTitle(dto.getTitle().trim());
        block.setStartDate(dto.getStartDate());
        block.setEndDate(dto.getEndDate());
        block.setStartTime(dto.getStartTime());
        block.setEndTime(dto.getEndTime());
        block.setReason(dto.getReason() != null ? dto.getReason().trim() : null);

        ScheduleBlock saved = blockRepository.save(block);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteBlock(Long id) {
        String specialty = getCurrentUserSpecialty();
        ScheduleBlock block = blockRepository.findByIdAndSpecialty(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo de agenda no encontrado"));
        blockRepository.delete(block);
    }

    @Transactional(readOnly = true)
    public void validateNoBlockConflict(LocalDate date, LocalTime startTime, LocalTime endTime, Long professionalId, String specialty) {
        if (date == null || startTime == null || endTime == null) {
            return;
        }

        List<ScheduleBlock> blocks = blockRepository.findBlocksForDate(specialty, professionalId, date);
        for (ScheduleBlock block : blocks) {
            // Si el bloqueo no tiene hora definida, bloquea todo el día
            if (block.getStartTime() == null || block.getEndTime() == null) {
                throw new IllegalArgumentException("La fecha seleccionada (" + date + ") coincide con un bloqueo de agenda (" + block.getTitle() + ").");
            }
            // Si tiene horario, verificar solapamiento: startTime < block.endTime && block.startTime < endTime
            if (startTime.isBefore(block.getEndTime()) && block.getStartTime().isBefore(endTime)) {
                throw new IllegalArgumentException("El horario solicitado (" + startTime + " - " + endTime + ") coincide con un bloqueo de agenda (" + block.getTitle() + ": " + block.getStartTime() + " - " + block.getEndTime() + ").");
            }
        }
    }

    private void validateBlockDates(ScheduleBlockDto dto) {
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("La fecha de inicio (" + dto.getStartDate() + ") no puede ser posterior a la fecha de fin (" + dto.getEndDate() + ").");
        }
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            if (!dto.getStartTime().isBefore(dto.getEndTime())) {
                throw new IllegalArgumentException("La hora de inicio (" + dto.getStartTime() + ") debe ser anterior a la hora de fin (" + dto.getEndTime() + ").");
            }
        }
    }

    private ScheduleBlockDto mapToDto(ScheduleBlock block) {
        ScheduleBlockDto dto = new ScheduleBlockDto();
        dto.setId(block.getId());
        dto.setProfessionalId(block.getProfessionalId());
        if (block.getProfessionalId() != null) {
            userRepository.findById(block.getProfessionalId()).ifPresent(u -> 
                dto.setProfessionalName(u.getFullName())
            );
        } else {
            dto.setProfessionalName("Toda la especialidad");
        }
        dto.setSpecialty(block.getSpecialty());
        dto.setTitle(block.getTitle());
        dto.setStartDate(block.getStartDate());
        dto.setEndDate(block.getEndDate());
        dto.setStartTime(block.getStartTime());
        dto.setEndTime(block.getEndTime());
        dto.setReason(block.getReason());
        return dto;
    }
}
