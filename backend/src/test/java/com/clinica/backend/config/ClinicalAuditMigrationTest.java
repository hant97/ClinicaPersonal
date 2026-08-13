package com.clinica.backend.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClinicalAuditMigrationTest {

    private static final String URL = "jdbc:h2:mem:clinical_audit_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    @Test
    void appliesToPrePhaseThreeSchemaAndIsRepeatableOnMigratedDatabase() throws Exception {
        createPrePhaseThreeSchema();
        Flyway flyway = Flyway.configure()
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("19")
                .target("20")
                .load();

        assertEquals(1, flyway.migrate().migrationsExecuted);
        assertAuditColumns("clinical_sessions", false);
        assertAuditColumns("medical_records", true);
        assertAuditColumns("dermatological_evaluations", false);
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    private void createPrePhaseThreeSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("CREATE TABLE clinical_sessions (id BIGINT, patient_id BIGINT, specialty VARCHAR(255), professional_id BIGINT)");
            statement.execute("CREATE TABLE medical_records (id BIGINT, patient_id BIGINT, specialty VARCHAR(255))");
            statement.execute("CREATE TABLE dermatological_evaluations (id BIGINT, patient_id BIGINT, professional_id BIGINT)");
        }
    }

    private void assertAuditColumns(String table, boolean includesProfessionalId) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, table, null)) {
            boolean deleted = false;
            boolean deletedAt = false;
            boolean deletedBy = false;
            boolean professionalId = !includesProfessionalId;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                deleted |= "deleted".equalsIgnoreCase(column);
                deletedAt |= "deleted_at".equalsIgnoreCase(column);
                deletedBy |= "deleted_by".equalsIgnoreCase(column);
                professionalId |= "professional_id".equalsIgnoreCase(column);
            }
            assertTrue(deleted);
            assertTrue(deletedAt);
            assertTrue(deletedBy);
            assertTrue(professionalId);
        }
    }
}
