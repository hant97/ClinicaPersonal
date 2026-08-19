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
    void appliesConsolidatedSchemaAndVerifiesAuditColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .load();

        assertEquals(4, flyway.migrate().migrationsExecuted);
        assertAuditColumns("clinical_sessions");
        assertAuditColumns("dermatological_evaluations");
        assertAuditColumns("general_history");
        assertAuditColumns("psychology_evaluations");
        assertAuditColumns("prescriptions");
        assertSpecialtiesTable();
        assertPatientUuidColumn();
        assertAppointmentGoogleColumns();
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    private void assertAppointmentGoogleColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "appointments", null)) {
            boolean hasGoogleEventId = false;
            boolean hasGoogleEventLink = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasGoogleEventId |= "google_event_id".equalsIgnoreCase(column);
                hasGoogleEventLink |= "google_event_link".equalsIgnoreCase(column);
            }
            assertTrue(hasGoogleEventId, "Column 'google_event_id' missing in appointments table");
            assertTrue(hasGoogleEventLink, "Column 'google_event_link' missing in appointments table");
        }
    }

    private void assertPatientUuidColumn() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "patients", null)) {
            boolean hasUuid = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasUuid |= "uuid".equalsIgnoreCase(column);
            }
            assertTrue(hasUuid, "Column 'uuid' missing in patients table");
        }
    }

    private void assertAuditColumns(String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, table, null)) {
            boolean deleted = false;
            boolean deletedAt = false;
            boolean deletedBy = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                deleted |= "deleted".equalsIgnoreCase(column);
                deletedAt |= "deleted_at".equalsIgnoreCase(column);
                deletedBy |= "deleted_by".equalsIgnoreCase(column);
            }
            assertTrue(deleted, "Column 'deleted' missing in " + table);
            assertTrue(deletedAt, "Column 'deleted_at' missing in " + table);
            assertTrue(deletedBy, "Column 'deleted_by' missing in " + table);
        }
    }

    private void assertSpecialtiesTable() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM specialties")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }
}
