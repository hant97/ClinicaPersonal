package com.clinica.backend.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertEquals(13, flyway.migrate().migrationsExecuted);
        assertAuditColumns("clinical_sessions");
        assertAuditColumns("dermatological_evaluations");
        assertAuditColumns("general_history");
        assertAuditColumns("psychology_evaluations");
        assertAuditColumns("prescriptions");
        assertAuditColumns("risk_assessments");
        assertSpecialtiesTable();
        assertPatientUuidColumn();
        assertAppointmentGoogleColumns();
        assertPaymentPhase0Columns();
        assertAppointmentReminderColumns();
        assertAuditLogTable();
        assertAgendaAdvancedPhase3();
        assertAttentionsPhase4();
        assertConcurrencyProtections();
        assertRiskAssessmentsTable();
        assertClinicalServiceCategoryCatalog();
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    private void assertClinicalServiceCategoryCatalog() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM catalog_items item " +
                             "JOIN catalogs catalog ON catalog.id = item.catalog_id " +
                             "WHERE catalog.code = 'CLINICAL_SERVICE_CATEGORY' " +
                             "AND catalog.specialty = 'GENERAL'")) {
            assertTrue(rs.next());
            assertEquals(6, rs.getInt(1));
        }
    }

    private void assertConcurrencyProtections() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO supplies(name, current_stock, min_stock_level, specialty) " +
                            "VALUES ('Inválido', -1, 0, 'PSICOLOGIA')"));
        }

        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet indexes = connection.getMetaData()
                     .getIndexInfo(null, null, "appointments", false, false)) {
            boolean hasConflictLookupIndex = false;
            while (indexes.next()) {
                hasConflictLookupIndex |= "idx_appointments_conflict_lookup"
                        .equalsIgnoreCase(indexes.getString("INDEX_NAME"));
            }
            assertTrue(hasConflictLookupIndex, "Appointment conflict lookup index is missing");
        }
    }

    private void assertAgendaAdvancedPhase3() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "appointments", null)) {
            boolean hasRecurrenceGroup = false;
            boolean hasRecurrenceRule = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasRecurrenceGroup |= "recurrence_group_id".equalsIgnoreCase(column);
                hasRecurrenceRule |= "recurrence_rule".equalsIgnoreCase(column);
            }
            assertTrue(hasRecurrenceGroup, "Column 'recurrence_group_id' missing in appointments table");
            assertTrue(hasRecurrenceRule, "Column 'recurrence_rule' missing in appointments table");
        }

        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'professional_schedules'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "Table 'professional_schedules' should exist");
        }

        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'schedule_blocks'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "Table 'schedule_blocks' should exist");
        }
    }

    private void assertAuditLogTable() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "audit_log", null)) {
            boolean hasId = false;
            boolean hasUserId = false;
            boolean hasUsername = false;
            boolean hasSpecialty = false;
            boolean hasAction = false;
            boolean hasEntityType = false;
            boolean hasEntityId = false;
            boolean hasDetail = false;
            boolean hasIp = false;
            boolean hasCreatedAt = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasId |= "id".equalsIgnoreCase(column);
                hasUserId |= "user_id".equalsIgnoreCase(column);
                hasUsername |= "username".equalsIgnoreCase(column);
                hasSpecialty |= "specialty".equalsIgnoreCase(column);
                hasAction |= "action".equalsIgnoreCase(column);
                hasEntityType |= "entity_type".equalsIgnoreCase(column);
                hasEntityId |= "entity_id".equalsIgnoreCase(column);
                hasDetail |= "detail".equalsIgnoreCase(column);
                hasIp |= "ip".equalsIgnoreCase(column);
                hasCreatedAt |= "created_at".equalsIgnoreCase(column);
            }
            assertTrue(hasId, "Column 'id' missing in audit_log table");
            assertTrue(hasUserId, "Column 'user_id' missing in audit_log table");
            assertTrue(hasUsername, "Column 'username' missing in audit_log table");
            assertTrue(hasSpecialty, "Column 'specialty' missing in audit_log table");
            assertTrue(hasAction, "Column 'action' missing in audit_log table");
            assertTrue(hasEntityType, "Column 'entity_type' missing in audit_log table");
            assertTrue(hasEntityId, "Column 'entity_id' missing in audit_log table");
            assertTrue(hasDetail, "Column 'detail' missing in audit_log table");
            assertTrue(hasIp, "Column 'ip' missing in audit_log table");
            assertTrue(hasCreatedAt, "Column 'created_at' missing in audit_log table");
        }
    }

    private void assertAppointmentReminderColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "appointments", null)) {
            boolean hasReminderSentAt = false;
            boolean hasConfirmationToken = false;
            boolean hasConfirmedAt = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasReminderSentAt |= "reminder_sent_at".equalsIgnoreCase(column);
                hasConfirmationToken |= "confirmation_token".equalsIgnoreCase(column);
                hasConfirmedAt |= "confirmed_at".equalsIgnoreCase(column);
            }
            assertTrue(hasReminderSentAt, "Column 'reminder_sent_at' missing in appointments table");
            assertTrue(hasConfirmationToken, "Column 'confirmation_token' missing in appointments table");
            assertTrue(hasConfirmedAt, "Column 'confirmed_at' missing in appointments table");
        }
    }

    private void assertPaymentPhase0Columns() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "payments", null)) {
            boolean hasStatus = false;
            boolean hasDueDate = false;
            boolean hasClinicalSessionId = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasStatus |= "status".equalsIgnoreCase(column);
                hasDueDate |= "due_date".equalsIgnoreCase(column);
                hasClinicalSessionId |= "clinical_session_id".equalsIgnoreCase(column);
            }
            assertTrue(hasStatus, "Column 'status' missing in payments table");
            assertTrue(hasDueDate, "Column 'due_date' missing in payments table");
            assertTrue(hasClinicalSessionId, "Column 'clinical_session_id' missing in payments table");
        }

        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'payment_transactions'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "Table 'payment_transactions' should exist");
        }

        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM payment_transactions")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "payment_transactions backfill expects no payments in a fresh schema");
        }
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

    private void assertRiskAssessmentsTable() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "risk_assessments", null)) {
            boolean hasPatientId = false;
            boolean hasClinicalSessionId = false;
            boolean hasRiskLevel = false;
            boolean hasSuicidalIdeation = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasPatientId |= "patient_id".equalsIgnoreCase(column);
                hasClinicalSessionId |= "clinical_session_id".equalsIgnoreCase(column);
                hasRiskLevel |= "risk_level".equalsIgnoreCase(column);
                hasSuicidalIdeation |= "suicidal_ideation".equalsIgnoreCase(column);
            }
            assertTrue(hasPatientId, "Column 'patient_id' missing in risk_assessments table");
            assertTrue(hasClinicalSessionId, "Column 'clinical_session_id' missing in risk_assessments table");
            assertTrue(hasRiskLevel, "Column 'risk_level' missing in risk_assessments table");
            assertTrue(hasSuicidalIdeation, "Column 'suicidal_ideation' missing in risk_assessments table");
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

    private void assertAttentionsPhase4() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'attentions'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "Table 'attentions' should exist");
        }

        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "attentions", null)) {
            boolean hasPatientId = false;
            boolean hasProfessionalId = false;
            boolean hasStatus = false;
            boolean hasAttentionDate = false;
            boolean hasDeleted = false;
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                hasPatientId |= "patient_id".equalsIgnoreCase(column);
                hasProfessionalId |= "professional_id".equalsIgnoreCase(column);
                hasStatus |= "status".equalsIgnoreCase(column);
                hasAttentionDate |= "attention_date".equalsIgnoreCase(column);
                hasDeleted |= "deleted".equalsIgnoreCase(column);
            }
            assertTrue(hasPatientId, "Column 'patient_id' missing in attentions table");
            assertTrue(hasProfessionalId, "Column 'professional_id' missing in attentions table");
            assertTrue(hasStatus, "Column 'status' missing in attentions table");
            assertTrue(hasAttentionDate, "Column 'attention_date' missing in attentions table");
            assertTrue(hasDeleted, "Column 'deleted' missing in attentions table");
        }

        String[] tablesWithAttentionId = {"appointments", "clinical_sessions", "prescriptions", "payments"};
        for (String table : tablesWithAttentionId) {
            try (Connection connection = DriverManager.getConnection(URL, "sa", "");
                 ResultSet columns = connection.getMetaData().getColumns(null, null, table, null)) {
                boolean hasAttentionId = false;
                while (columns.next()) {
                    String column = columns.getString("COLUMN_NAME");
                    hasAttentionId |= "attention_id".equalsIgnoreCase(column);
                }
                assertTrue(hasAttentionId, "Column 'attention_id' missing in " + table + " table");
            }
        }
    }
}
