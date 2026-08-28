package com.clinica.backend.service;

import com.clinica.backend.mapper.ProfessionalScheduleMapper;

import com.clinica.backend.dto.ProfessionalScheduleDto;
import com.clinica.backend.dto.WeeklyScheduleDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ProfessionalSchedule;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ProfessionalScheduleRepository;
import com.clinica.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalScheduleService {

    private final ProfessionalScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ProfessionalScheduleMapper professionalScheduleMapper;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getCurrentUserSpecialty() {
        return getCurrentUser().getSpecialty();
    }

    @Transactional(readOnly = true)
    public WeeklyScheduleDto getWeeklySchedule(Long professionalId) {
        String specialty = getCurrentUserSpecialty();
        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        List<ProfessionalSchedule> entities = scheduleRepository
                .findByProfessionalIdAndSpecialtyOrderByDayOfWeekAscStartTimeAsc(professionalId, specialty);

        List<ProfessionalScheduleDto> dtos = entities.stream()
                .map(professionalScheduleMapper::toDto)
                .collect(Collectors.toList());

        String name = (professional.getFirstName() + " " + professional.getLastName()).trim();
        return new WeeklyScheduleDto(professionalId, name, specialty, dtos);
    }

    @Transactional
    public WeeklyScheduleDto saveWeeklySchedule(WeeklyScheduleDto dto) {
        String specialty = getCurrentUserSpecialty();
        Long professionalId = dto.getProfessionalId();
        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        // Validar cada franja
        if (dto.getSchedules() != null) {
            for (ProfessionalScheduleDto item : dto.getSchedules()) {
                if (item.getDayOfWeek() == null || item.getDayOfWeek() < 1 || item.getDayOfWeek() > 7) {
                    throw new IllegalArgumentException("Día de la semana inválido (debe ser entre 1 y 7).");
                }
                if (item.getStartTime() == null || item.getEndTime() == null) {
                    throw new IllegalArgumentException("Las horas de inicio y fin son obligatorias para cada franja.");
                }
                if (!item.getStartTime().isBefore(item.getEndTime())) {
                    throw new IllegalArgumentException("La hora de inicio (" + item.getStartTime() + ") debe ser anterior a la hora de fin (" + item.getEndTime() + ").");
                }
            }
        }

        // Eliminar horarios anteriores para este profesional y especialidad
        scheduleRepository.deleteByProfessionalIdAndSpecialty(professionalId, specialty);

        List<ProfessionalSchedule> toSave = new ArrayList<>();
        if (dto.getSchedules() != null) {
            for (ProfessionalScheduleDto item : dto.getSchedules()) {
                ProfessionalSchedule schedule = new ProfessionalSchedule();
                schedule.setProfessionalId(professionalId);
                schedule.setDayOfWeek(item.getDayOfWeek());
                schedule.setStartTime(item.getStartTime());
                schedule.setEndTime(item.getEndTime());
                schedule.setActive(item.isActive());
                schedule.setSpecialty(specialty);
                toSave.add(schedule);
            }
        }

        List<ProfessionalSchedule> saved = scheduleRepository.saveAll(toSave);
        List<ProfessionalScheduleDto> resultDtos = saved.stream()
                .map(professionalScheduleMapper::toDto)
                .collect(Collectors.toList());

        String name = (professional.getFirstName() + " " + professional.getLastName()).trim();
        return new WeeklyScheduleDto(professionalId, name, specialty, resultDtos);
    }

    @Transactional(readOnly = true)
    public boolean isProfessionalAvailable(Long professionalId, LocalDate date, LocalTime startTime, LocalTime endTime, String specialty) {
        if (professionalId == null || date == null || startTime == null || endTime == null) {
            return true;
        }

        boolean hasConfiguredSchedules = scheduleRepository.existsByProfessionalIdAndSpecialtyAndIsActiveTrue(professionalId, specialty);
        if (!hasConfiguredSchedules) {
            // Si no hay configuración de horarios para el profesional, se mantiene comportamiento permisivo
            return true;
        }

        int dayOfWeek = date.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        List<ProfessionalSchedule> activeSchedules = scheduleRepository
                .findByProfessionalIdAndSpecialtyAndDayOfWeekAndIsActiveTrue(professionalId, specialty, dayOfWeek);

        if (activeSchedules.isEmpty()) {
            return false;
        }

        // Debe caer dentro de al menos un rango activo
        return activeSchedules.stream().anyMatch(s -> 
            !startTime.isBefore(s.getStartTime()) && !endTime.isAfter(s.getEndTime())
        );
    }

    public void validateProfessionalAvailability(Long professionalId, LocalDate date, LocalTime startTime, LocalTime endTime, String specialty) {
        if (!isProfessionalAvailable(professionalId, date, startTime, endTime, specialty)) {
            throw new IllegalArgumentException("El horario solicitado (" + startTime + " - " + endTime + ") en la fecha " + date + " está fuera de la jornada laboral activa del profesional.");
        }
    }

}
