package com.clinica.backend.service;

import com.clinica.backend.dto.AuditLogDto;
import com.clinica.backend.model.AuditLog;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String entityType, String entityId, String detail) {
        User currentUser = resolveCurrentUser();
        String ip = resolveClientIp();
        record(
                currentUser != null ? currentUser.getId() : null,
                currentUser != null ? currentUser.getUsername() : null,
                currentUser != null ? currentUser.getSpecialty() : null,
                action,
                entityType,
                entityId,
                detail,
                ip
        );
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String entityType, String entityId, String detail, User user, String ip) {
        record(
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getSpecialty() : null,
                action,
                entityType,
                entityId,
                detail,
                ip != null ? ip : resolveClientIp()
        );
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(Long userId, String username, String specialty, String action,
                       String entityType, String entityId, String detail, String ip) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .specialty(specialty)
                    .action(action != null ? action.toUpperCase() : "UNKNOWN")
                    .entityType(entityType != null ? entityType.toUpperCase() : "GENERAL")
                    .entityId(entityId)
                    .detail(detail)
                    .ip(ip)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría para [action={}, entityType={}, entityId={}]: {}",
                    action, entityType, entityId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(
            LocalDate startDate,
            LocalDate endDate,
            String username,
            String specialty,
            String action,
            String entityType,
            String query,
            Pageable pageable
    ) {
        Specification<AuditLog> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
            }
            if (specialty != null && !specialty.isBlank()) {
                predicates.add(cb.equal(root.get("specialty"), specialty.trim().toUpperCase()));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim().toUpperCase()));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim().toUpperCase()));
            }
            if (query != null && !query.isBlank()) {
                String searchPattern = "%" + query.trim().toLowerCase() + "%";
                Predicate detailMatch = cb.like(cb.lower(root.get("detail")), searchPattern);
                Predicate usernameMatch = cb.like(cb.lower(root.get("username")), searchPattern);
                Predicate entityIdMatch = cb.like(cb.lower(root.get("entityId")), searchPattern);
                predicates.add(cb.or(detailMatch, usernameMatch, entityIdMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctActions() {
        return auditLogRepository.findDistinctActions();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctEntityTypes() {
        return auditLogRepository.findDistinctEntityTypes();
    }

    public AuditLogDto mapToDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .username(auditLog.getUsername())
                .specialty(auditLog.getSpecialty())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .detail(auditLog.getDetail())
                .ip(auditLog.getIp())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private User resolveCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                return user;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveClientIp() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                return extractClientIp(request);
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    public String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("unknown")) {
                    return trimmed;
                }
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !xRealIp.equalsIgnoreCase("unknown")) {
            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "127.0.0.1";
    }
}
