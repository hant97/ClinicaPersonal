package com.clinica.backend.config;

import com.clinica.backend.model.*;
import com.clinica.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
public class MockDataSeeder {

    @Bean
    CommandLineRunner generateMockData(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            PaymentRepository paymentRepository,
            ClinicalServiceRepository clinicalServiceRepository,
            SupplyRepository supplyRepository,
            PsychometricTestRepository psychometricTestRepository,
            MedicalRecordRepository medicalRecordRepository,
            RiskAlertRepository riskAlertRepository,
            ClinicalSessionRepository clinicalSessionRepository,
            AssessmentRepository assessmentRepository) {
        
        return args -> {
            if (patientRepository.count() > 0) {
                System.out.println("Los datos de prueba ya existen. Saltando generación de datos mock.");
                return;
            }

            System.out.println("Generando datos de prueba...");

            // 1. Crear 4 Servicios
            List<ClinicalService> services = new ArrayList<>();
            String[] serviceNames = {"Consulta Psicológica General", "Terapia de Pareja", "Evaluación Psicométrica", "Orientación Vocacional"};
            double[] servicePrices = {50.00, 80.00, 100.00, 60.00};
            for (int i = 0; i < 4; i++) {
                ClinicalService service = new ClinicalService();
                service.setName(serviceNames[i]);
                service.setDescription("Descripción para " + serviceNames[i]);
                service.setPrice(BigDecimal.valueOf(servicePrices[i]));
                services.add(clinicalServiceRepository.save(service));
            }

            // 2. Crear 5 Insumos
            List<Supply> supplies = new ArrayList<>();
            String[] supplyNames = {"Cuadernillos de Test de Rorschach", "Hojas de respuesta MMPI", "Lápices HB", "Papel bond A4 (resma)", "Kits de relajación guiada"};
            for (int i = 0; i < 5; i++) {
                Supply supply = new Supply();
                supply.setName(supplyNames[i]);
                supply.setDescription("Insumo de prueba " + (i + 1));
                supply.setCurrentStock(20 + (i * 10));
                supply.setMinStockLevel(10);
                supply.setUnit(i < 2 ? "Unidades" : (i == 3 ? "Resmas" : "Cajas"));
                supply.setPrice(BigDecimal.valueOf(5.00 + i));
                supply.setExpirationDate(LocalDate.now().plusMonths(6 + i));
                supplies.add(supplyRepository.save(supply));
            }

            // 3. Crear 3 Tests Psicométricos
            List<PsychometricTest> tests = new ArrayList<>();
            String[] testNames = {"Test de Personalidad MMPI-2", "Test de Rorschach", "Escala de Ansiedad de Hamilton (HAM-A)"};
            for (int i = 0; i < 3; i++) {
                PsychometricTest test = new PsychometricTest();
                test.setName(testNames[i]);
                test.setDescription("Evaluación clínica estructurada para el test " + testNames[i]);
                test.setQuestionsJson("[{\"id\": 1, \"text\": \"Pregunta de ejemplo\", \"options\": [{\"score\": 0, \"text\": \"Falso\"}, {\"score\": 1, \"text\": \"Verdadero\"}]}]");
                tests.add(psychometricTestRepository.save(test));
            }

            // 4. Crear 5 Pacientes con todos sus detalles (Historial, Alertas, Sesiones, Evaluaciones)
            List<Patient> patients = new ArrayList<>();
            String[] firstNames = {"Juan", "María", "Carlos", "Lucía", "Pedro"};
            String[] lastNames = {"Pérez", "González", "López", "Martínez", "Sánchez"};
            Random random = new Random();

            for (int i = 0; i < 5; i++) {
                Patient patient = new Patient();
                patient.setFirstName(firstNames[i]);
                patient.setLastName(lastNames[i]);
                patient.setIdentificationDocument("12345678" + i);
                patient.setEmail("paciente" + i + "@ejemplo.com");
                patient.setContactNumber("98765432" + i);
                patient.setDateOfBirth(LocalDate.of(1980 + (i * 5), 1 + i, 10 + i));
                patient.setReasonForConsultation("Consulta inicial por estrés");
                patient.setGender(i % 2 == 0 ? "Masculino" : "Femenino");
                patient.setOccupation("Profesional independiente");
                patient.setMaritalStatus("Soltero(a)");
                patient = patientRepository.save(patient);
                patients.add(patient);

                // A. Historial de Tratamientos (MedicalRecord)
                MedicalRecord record = new MedicalRecord();
                record.setPatient(patient);
                record.setDiagnosis("Ansiedad Generalizada");
                record.setCurrentMedication("Ninguna");
                record.setTreatmentPlan("Terapia cognitivo-conductual semanal");
                record.setTreatmentStatus("Activo");
                medicalRecordRepository.save(record);

                // B. Alertas de Riesgo Activas (RiskAlert)
                RiskAlert alert = new RiskAlert();
                alert.setPatientId(patient.getId());
                alert.setType(i % 2 == 0 ? "Riesgo de autolesión" : "Alergia severa");
                alert.setLevel(i % 2 == 0 ? "Alto" : "Medio");
                alert.setDescription("El paciente requiere monitoreo constante.");
                alert.setActive(true);
                riskAlertRepository.save(alert);

                // C. Historia Clínica (Sesiones) (ClinicalSession)
                ClinicalSession session = new ClinicalSession();
                session.setPatient(patient);
                session.setSessionDate(LocalDate.now().minusDays(2 + i));
                session.setStartTime(LocalTime.of(10, 0));
                session.setEndTime(LocalTime.of(11, 0));
                session.setSessionType("Terapia Individual");
                session.setModality(i % 2 == 0 ? "PRESENCIAL" : "VIRTUAL");
                session.setStatus("COMPLETADA");
                session.setSubjective("El paciente reporta sentirse más tranquilo.");
                session.setObjective("Expresión facial relajada, buen contacto visual.");
                session.setAnalysis("Avance positivo en el control de la ansiedad.");
                session.setPlan("Continuar con ejercicios de respiración.");
                session.setConfidential(false);
                clinicalSessionRepository.save(session);

                // D. Evaluaciones Psicométricas (Assessment)
                Assessment assessment = new Assessment();
                assessment.setPatient(patient);
                assessment.setPsychometricTest(tests.get(random.nextInt(tests.size())));
                assessment.setTotalScore(random.nextInt(50) + 10);
                assessment.setAnswersJson("{\"1\": 1}");
                assessment.setNotes("Evaluación realizada sin inconvenientes.");
                assessmentRepository.save(assessment);
            }

            // 5. Crear 10 Citas (divididas en distintos estados)
            String[] statuses = {"PROGRAMADA", "CONFIRMADA", "COMPLETADA", "CANCELADA", "NO_ASISTIO", "PROGRAMADA", "COMPLETADA", "CONFIRMADA", "CANCELADA", "COMPLETADA"};
            String[] modalities = {"PRESENCIAL", "VIRTUAL", "PRESENCIAL", "PRESENCIAL", "VIRTUAL", "VIRTUAL", "PRESENCIAL", "PRESENCIAL", "VIRTUAL", "PRESENCIAL"};
            for (int i = 0; i < 10; i++) {
                Appointment appointment = new Appointment();
                Patient assignedPatient = patients.get(i % patients.size());
                appointment.setPatient(assignedPatient);
                appointment.setAppointmentDate(LocalDate.now().plusDays(i - 3)); // Algunas en el pasado, otras en el futuro
                appointment.setStartTime(LocalTime.of(9 + i, 0));
                appointment.setEndTime(LocalTime.of(10 + i, 0));
                appointment.setStatus(statuses[i]);
                appointment.setModality(modalities[i]);
                appointment.setFirstTime(i % 3 == 0);
                appointment.setProfessionalId(1L);
                appointment.setNotes("Nota para cita de " + assignedPatient.getFirstName());
                if ("VIRTUAL".equals(modalities[i])) {
                    appointment.setVideoCallLink("https://meet.google.com/abc-defg-hij");
                }
                appointmentRepository.save(appointment);
            }

            // 6. Crear 5 Facturaciones
            for (int i = 0; i < 5; i++) {
                Patient patient = patients.get(i);
                Payment payment = new Payment();
                payment.setPatient(patient);
                payment.setPaymentDate(LocalDateTime.now().minusDays(i));
                payment.setPaymentMethod(i % 2 == 0 ? "CASH" : "TRANSFER");
                payment.setDescription("Facturación de servicios y/o insumos");
                
                BigDecimal totalAmount = BigDecimal.ZERO;
                int numItems = (i % 2) + 1; 
                
                for (int j = 0; j < numItems; j++) {
                    PaymentItem item = new PaymentItem();
                    item.setPayment(payment);
                    item.setQuantity(1);
                    
                    if (j == 0) {
                        ClinicalService service = services.get(random.nextInt(services.size()));
                        item.setClinicalService(service);
                        item.setDescription("Servicio: " + service.getName());
                        item.setUnitPrice(service.getPrice());
                        item.setTotalPrice(service.getPrice());
                        totalAmount = totalAmount.add(item.getTotalPrice());
                    } else {
                        Supply supply = supplies.get(random.nextInt(supplies.size()));
                        item.setSupply(supply);
                        item.setDescription("Insumo: " + supply.getName());
                        item.setUnitPrice(supply.getPrice());
                        item.setTotalPrice(supply.getPrice());
                        totalAmount = totalAmount.add(item.getTotalPrice());
                    }
                    payment.getItems().add(item);
                }
                
                payment.setAmount(totalAmount);
                paymentRepository.save(payment);
            }

            System.out.println("Generación de datos mock completada exitosamente.");
        };
    }
}
