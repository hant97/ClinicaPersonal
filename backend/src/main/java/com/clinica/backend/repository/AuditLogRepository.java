package com.clinica.backend.repository;

import com.clinica.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT DISTINCT a.action FROM AuditLog a ORDER BY a.action ASC")
    List<String> findDistinctActions();

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a ORDER BY a.entityType ASC")
    List<String> findDistinctEntityTypes();
}
