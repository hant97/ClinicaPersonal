package com.clinica.backend.config;

import com.clinica.backend.model.*;
import com.clinica.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
@Profile("dev & !test")
public class MockDataSeeder {

    @Bean
    CommandLineRunner generateMockData(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            PaymentRepository paymentRepository,
            ClinicalServiceRepository clinicalServiceRepository,
            SupplyRepository supplyRepository,
            PsychometricTestRepository psychometricTestRepository,
            RiskAlertRepository riskAlertRepository,
            ClinicalSessionRepository clinicalSessionRepository,
            AssessmentRepository assessmentRepository,
            DermatologicalEvaluationRepository dermatologicalEvaluationRepository,
            GeneralHistoryRepository generalHistoryRepository,
            AllergyRepository allergyRepository,
            MedicationRepository medicationRepository,
            PsychologyEvaluationRepository psychologyEvaluationRepository,
            DiagnosisRepository diagnosisRepository,
            TherapeuticPlanRepository therapeuticPlanRepository,
            DermatologicalHistoryRepository dermatologicalHistoryRepository,
            LesionRepository lesionRepository,
            AuxiliaryExamRepository auxiliaryExamRepository,
            TreatmentRepository treatmentRepository,
            ProcedureRepository procedureRepository,
            EvolutionRepository evolutionRepository) {

        return args -> {
            if (patientRepository.count() > 0) {
                System.out.println("Los datos de prueba ya existen. Saltando generación de datos mock.");
                return;
            }

            System.out.println("Generando datos de prueba...");

            // 1. Crear 4 Servicios
            List<ClinicalService> services = new ArrayList<>();
            String[] serviceNames = {"Consulta Psicológica General", "Terapia de Pareja", "Consulta Dermatológica", "Limpieza Facial Profunda"};
            double[] servicePrices = {50.00, 80.00, 100.00, 60.00};
            String[] serviceSpecialties = {"PSICOLOGIA", "PSICOLOGIA", "DERMATOLOGIA", "DERMATOLOGIA"};
            for (int i = 0; i < 4; i++) {
                ClinicalService service = new ClinicalService();
                service.setName(serviceNames[i]);
                service.setDescription("Descripción para " + serviceNames[i]);
                service.setPrice(BigDecimal.valueOf(servicePrices[i]));
                service.setSpecialty(serviceSpecialties[i]);
                services.add(clinicalServiceRepository.save(service));
            }

            // 2. Crear 5 Insumos
            List<Supply> supplies = new ArrayList<>();
            String[] supplyNames = {"Cuadernillos de Test de Rorschach", "Hojas de respuesta MMPI", "Lápices HB", "Crema Hidratante", "Ácido Salicílico 2%"};
            String[] supplySpecialties = {"PSICOLOGIA", "PSICOLOGIA", "PSICOLOGIA", "DERMATOLOGIA", "DERMATOLOGIA"};
            for (int i = 0; i < 5; i++) {
                Supply supply = new Supply();
                supply.setName(supplyNames[i]);
                supply.setDescription("Insumo de prueba " + (i + 1));
                supply.setCurrentStock(20 + (i * 10));
                supply.setMinStockLevel(10);
                supply.setUnit(i < 2 ? "Unidades" : (i == 3 ? "Botes" : "Cajas"));
                supply.setPrice(BigDecimal.valueOf(5.00 + i));
                supply.setExpirationDate(LocalDate.now().plusMonths(6 + i));
                supply.setSpecialty(supplySpecialties[i]);
                supplies.add(supplyRepository.save(supply));
            }

            // 3. Crear 3 Tests Psicométricos
            List<PsychometricTest> tests = new ArrayList<>();
            String[] testNames = {"Test de Personalidad MMPI-2", "Test de Rorschach", "Escala de Ansiedad de Hamilton (HAM-A)"};
            String[] testInterpretations = {
                "[{\"minScore\":0,\"maxScore\":25,\"label\":\"Bajo\",\"color\":\"emerald\",\"description\":\"Puntaje dentro del rango bajo.\"},{\"minScore\":26,\"maxScore\":50,\"label\":\"Moderado\",\"color\":\"amber\",\"description\":\"Puntaje dentro del rango moderado.\"},{\"minScore\":51,\"maxScore\":100,\"label\":\"Elevado\",\"color\":\"red\",\"description\":\"Puntaje dentro del rango elevado.\"}]",
                "[{\"minScore\":0,\"maxScore\":25,\"label\":\"Bajo\",\"color\":\"emerald\",\"description\":\"Puntaje dentro del rango bajo.\"},{\"minScore\":26,\"maxScore\":50,\"label\":\"Moderado\",\"color\":\"amber\",\"description\":\"Puntaje dentro del rango moderado.\"},{\"minScore\":51,\"maxScore\":100,\"label\":\"Elevado\",\"color\":\"red\",\"description\":\"Puntaje dentro del rango elevado.\"}]",
                "[{\"minScore\":0,\"maxScore\":13,\"label\":\"Sin ansiedad\",\"color\":\"emerald\",\"description\":\"No se observan síntomas de ansiedad significativos.\"},{\"minScore\":14,\"maxScore\":17,\"label\":\"Leve\",\"color\":\"blue\",\"description\":\"Síntomas de ansiedad leves.\"},{\"minScore\":18,\"maxScore\":24,\"label\":\"Moderada\",\"color\":\"orange\",\"description\":\"Síntomas de ansiedad moderados.\"},{\"minScore\":25,\"maxScore\":56,\"label\":\"Severa\",\"color\":\"red\",\"description\":\"Síntomas de ansiedad severos.\"}]"
            };
            for (int i = 0; i < 3; i++) {
                PsychometricTest test = new PsychometricTest();
                test.setName(testNames[i]);
                test.setDescription("Evaluación clínica estructurada para el test " + testNames[i]);
                test.setQuestionsJson("[{\"id\": 1, \"text\": \"Pregunta de ejemplo\", \"options\": [{\"score\": 0, \"text\": \"Falso\"}, {\"score\": 1, \"text\": \"Verdadero\"}]}]");
                test.setInterpretationJson(testInterpretations[i]);
                tests.add(psychometricTestRepository.save(test));
            }

            // 4. Crear 5 Pacientes con todos sus detalles (Historial, Alertas, Sesiones, Evaluaciones)
            List<Patient> patients = new ArrayList<>();
            String[] firstNames = {"Juan", "María", "Carlos", "Lucía", "Pedro"};
            String[] lastNames = {"Pérez", "González", "López", "Martínez", "Sánchez"};
            Random random = new Random();

            // Datos de ejemplo para variar la historia clínica de psicología
            String[] psychDiagnoses = {"Trastorno de ansiedad generalizada", "Depresión moderada", "Trastorno de pánico", "Estrés postraumático", "Trastorno obsesivo-compulsivo"};
            String[] psychCategories = {"Trastornos de ansiedad", "Trastornos del estado de ánimo", "Trastornos de ansiedad", "Trastornos relacionados con trauma", "Trastornos obsesivos y relacionados"};
            String[] psychSecondDiagnoses = {"Insomnio crónico", "Trastorno de adaptación", "Agorafobia", "Trastorno mixto ansioso-depresivo", "Trastorno de ansiedad social"};
            String[] psychAllergens1 = {"Penicilina", "Polen", "Ácaros", "Sulfas", "AINEs"};
            String[] psychAllergens2 = {"Látex", "Epitelio de gato", "Mariscos", "Aspirina", "Polvo doméstico"};
            String[] psychMeds1 = {"Sertralina", "Fluoxetina", "Escitalopram", "Paroxetina", "Venlafaxina"};
            String[] psychMeds2 = {"Clonazepam", "Alprazolam", "Quetiapina", "Lorazepam", "Bromazepam"};

            // Datos de ejemplo para variar la historia clínica de dermatología
            String[] dermDiagnoses = {"Dermatitis atópica leve-moderada", "Acné vulgar", "Psoriasis en placas", "Rosácea eritematotelangiectásica", "Melasma"};
            String[] dermCategories = {"Dermatitis", "Acné", "Psoriasis", "Rosácea", "Trastornos de pigmentación"};
            String[] dermSecondDiagnoses = {"Dermatitis de contacto", "Hiperpigmentación postinflamatoria", "Queratosis actínica", "Alopecia areata", "Urticaria crónica"};
            String[] dermAllergens1 = {"Corticosteroides tópicos", "Níquel", "Fragancias", "Lanolina", "Parabenos"};
            String[] dermAllergens2 = {"Peróxido de benzoilo", "Ácido salicílico", "Sulfatos", "Aloe vera", "Isotiazolinonas"};
            String[] dermMeds1 = {"Loratadina", "Cetirizina", "Desloratadina", "Hidroxicina", "Fexofenadina"};
            String[] dermTreatments1 = {"Emolientes + corticosteroide tópico", "Isotretinoína oral", "Metotrexato", "Metronidazol tópico", "Hidroquinona tópica"};
            String[] dermTreatments2 = {"Antihistamínico oral", "Peróxido de benzoilo tópico", "Calcipotriol tópico", "Ácido azelaico tópico", "Protector solar SPF 50"};
            String[] dermProcedures1 = {"Dermatoscopia", "Crioterapia", "Biopsia cutánea", "Limpieza facial profunda", "Peeling químico"};
            String[] dermProcedures2 = {"Extirpación de lesión", "Terapia fotodinámica", "Infiltración intralesional", "Drenaje de absceso", "Electrocoagulación"};
            String[] dermExamTypes = {"Dermatoscopia", "Biopsia de piel", "Cultivo micológico", "Prueba de parche", "Examen de laboratorio"};
            String[] dermBodyAreas = {"Pliegues de flexión", "Rostro", "Codos y rodillas", "Mejillas", "Frente"};
            String[] dermLesionTypes = {"Eritema y descamación", "Comedones y pápulas", "Placas eritematosas", "Pápulas y pústulas", "Máculas hiperpigmentadas"};

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
                patient.setSpecialty("PSICOLOGIA");
                patient = patientRepository.save(patient);
                patients.add(patient);

                // A. Bloques generales (nueva historia clínica)
                GeneralHistory generalHistory = new GeneralHistory();
                generalHistory.setPatient(patient);
                generalHistory.setSpecialty("PSICOLOGIA");
                generalHistory.setPathologicalHistory(i == 0 ? "Hipertensión arterial controlada." : "Sin antecedentes patológicos relevantes.");
                generalHistory.setSurgicalHistory(i < 2 ? "Apendicectomía a los 20 años." : "Sin cirugías previas.");
                generalHistory.setFamilyHistory("Madre con antecedentes de " + (i % 2 == 0 ? "ansiedad" : "depresión") + ".");
                generalHistory.setHabits(i % 2 == 0 ? "Sedentario, sueño irregular, consumo moderado de cafeína." : "Actividad física ocasional, tabaquismo social.");
                generalHistory.setNotes("Paciente " + patient.getFirstName() + " en seguimiento ambulatorio.");
                generalHistoryRepository.save(generalHistory);

                Allergy allergy1 = new Allergy();
                allergy1.setPatient(patient);
                allergy1.setSpecialty("PSICOLOGIA");
                allergy1.setAllergen(psychAllergens1[i]);
                allergy1.setType("Medicamento");
                allergy1.setSeverity("Leve");
                allergy1.setReaction("Erupción cutánea");
                allergy1.setNotes("Confirmada por reporte del paciente.");
                allergyRepository.save(allergy1);

                Allergy allergy2 = new Allergy();
                allergy2.setPatient(patient);
                allergy2.setSpecialty("PSICOLOGIA");
                allergy2.setAllergen(psychAllergens2[i]);
                allergy2.setType(i % 2 == 0 ? "Ambiental" : "Alimento");
                allergy2.setSeverity("Moderada");
                allergy2.setReaction("Rinitis y prurito");
                allergy2.setActive(i != 4);
                allergyRepository.save(allergy2);

                Medication med1 = new Medication();
                med1.setPatient(patient);
                med1.setSpecialty("PSICOLOGIA");
                med1.setName(psychMeds1[i]);
                med1.setDose("50 mg");
                med1.setFrequency("1 vez al día");
                med1.setStartDate(LocalDate.now().minusMonths(3));
                med1.setNotes("En curso, con buena tolerancia.");
                medicationRepository.save(med1);

                Medication med2 = new Medication();
                med2.setPatient(patient);
                med2.setSpecialty("PSICOLOGIA");
                med2.setName(psychMeds2[i]);
                med2.setDose("0.5 mg");
                med2.setFrequency("Solo si es necesario");
                med2.setStartDate(LocalDate.now().minusMonths(1));
                med2.setActive(i != 2);
                medicationRepository.save(med2);

                // B. Psicología (nueva historia clínica)
                PsychologyEvaluation psychEval = new PsychologyEvaluation();
                psychEval.setPatient(patient);
                psychEval.setEvaluationDate(LocalDate.now().minusDays(5 + i));
                psychEval.setInitialEvaluation("Paciente acude por síntomas compatibles con " + psychDiagnoses[i].toLowerCase() + ".");
                psychEval.setPsychologicalHistory("Episodios previos de ansiedad en contexto laboral y familiar.");
                psychEval.setMentalExam("Consciente, orientado en tiempo y espacio, afecto ansioso, discurso coherente.");
                psychEval.setNotes("Se sugiere seguimiento semanal.");
                psychologyEvaluationRepository.save(psychEval);

                Diagnosis diag1 = new Diagnosis();
                diag1.setPatient(patient);
                diag1.setSpecialty("PSICOLOGIA");
                diag1.setCategory(psychCategories[i]);
                diag1.setDescription(psychDiagnoses[i]);
                diag1.setStatus("ACTIVO");
                diag1.setDiagnosisDate(LocalDate.now().minusDays(5 + i));
                diagnosisRepository.save(diag1);

                Diagnosis diag2 = new Diagnosis();
                diag2.setPatient(patient);
                diag2.setSpecialty("PSICOLOGIA");
                diag2.setCategory("Otros problemas clínicos");
                diag2.setDescription(psychSecondDiagnoses[i]);
                diag2.setStatus(i % 2 == 0 ? "ACTIVO" : "RESUELTO");
                diag2.setDiagnosisDate(LocalDate.now().minusMonths(1));
                diag2.setNotes("Diagnóstico secundario.");
                diagnosisRepository.save(diag2);

                TherapeuticPlan plan = new TherapeuticPlan();
                plan.setPatient(patient);
                plan.setSpecialty("PSICOLOGIA");
                plan.setObjectives("Reducir síntomas de " + psychDiagnoses[i].toLowerCase() + " y mejorar estrategias de afrontamiento.");
                plan.setInterventions("Terapia cognitivo-conductual, psicoeducación y entrenamiento en relajación.");
                plan.setStartDate(LocalDate.now().minusMonths(1));
                plan.setStatus("ACTIVO");
                plan.setNotes("Reevaluar objetivos en 6 semanas.");
                therapeuticPlanRepository.save(plan);

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
                session.setSpecialty("PSICOLOGIA");
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

            // 5. Crear 5 Pacientes de Dermatología con historia clínica completa
            String[] dermFirstNames = {"Valentina", "Andrés", "Camila", "Diego", "Fernanda"};
            String[] dermLastNames = {"Rojas", "Torres", "Vega", "Castro", "Mendoza"};
            for (int i = 0; i < 5; i++) {
                Patient patient = new Patient();
                patient.setFirstName(dermFirstNames[i]);
                patient.setLastName(dermLastNames[i]);
                patient.setIdentificationDocument("23456789" + i);
                patient.setEmail("pacientederm" + i + "@ejemplo.com");
                patient.setContactNumber("97654321" + i);
                patient.setDateOfBirth(LocalDate.of(1985 + (i * 4), 2 + i, 5 + i));
                patient.setReasonForConsultation("Evaluación y tratamiento de lesiones en la piel");
                patient.setGender(i % 2 == 0 ? "Femenino" : "Masculino");
                patient.setOccupation("Independiente");
                patient.setMaritalStatus("Soltero(a)");
                patient.setSpecialty("DERMATOLOGIA");
                patient = patientRepository.save(patient);
                patients.add(patient);

                // A. Historia Clínica (Sesión) - Dermatología
                ClinicalSession session = new ClinicalSession();
                session.setPatient(patient);
                session.setSessionDate(LocalDate.now().minusDays(3 + i));
                session.setStartTime(LocalTime.of(9, 30));
                session.setEndTime(LocalTime.of(10, 15));
                session.setSessionType("Consulta Dermatológica");
                session.setModality(i % 2 == 0 ? "PRESENCIAL" : "VIRTUAL");
                session.setStatus("COMPLETADA");
                session.setSubjective("Paciente refiere prurito y resequedad en zonas de flexión.");
                session.setObjective("Eritema y descamación en pliegues antecubitales y poplíteos.");
                session.setAnalysis("Cuadro compatible con dermatitis atópica en actividad leve-moderada.");
                session.setPlan("Mantener emolientes y corticosteroide tópico; control en 4 semanas.");
                session.setConfidential(false);
                session.setSpecialty("DERMATOLOGIA");
                clinicalSessionRepository.save(session);

                // C. Evaluación Dermatológica
                DermatologicalEvaluation dermEval = new DermatologicalEvaluation();
                dermEval.setPatient(patient);
                dermEval.setEvaluationDate(LocalDate.now().minusDays(1 + i));
                dermEval.setSkinType("Sensible");
                dermEval.setAffectedArea("Pliegues de flexión");
                dermEval.setLesionType("Eritema y descamación");
                dermEval.setLesionSize("Placas en áreas de flexión");
                dermEval.setDermatologicalDiagnosis("Dermatitis atópica leve-moderada");
                dermEval.setTreatmentIndicated("Emolientes diarios y corticosteroide tópico por brotes.");
                dermEval.setProcedurePerformed("Dermatoscopia");
                dermEval.setEvolutionNotes("Buena respuesta inicial al tratamiento tópico.");
                dermEval.setNextReviewDate(LocalDate.now().plusDays(28));
                dermatologicalEvaluationRepository.save(dermEval);

                // C2. Bloques generales (nueva historia clínica)
                Allergy dermAllergy1 = new Allergy();
                dermAllergy1.setPatient(patient);
                dermAllergy1.setSpecialty("DERMATOLOGIA");
                dermAllergy1.setAllergen(dermAllergens1[i]);
                dermAllergy1.setType("Contacto");
                dermAllergy1.setSeverity("Moderada");
                dermAllergy1.setReaction("Irritación local y eritema");
                allergyRepository.save(dermAllergy1);

                Allergy dermAllergy2 = new Allergy();
                dermAllergy2.setPatient(patient);
                dermAllergy2.setSpecialty("DERMATOLOGIA");
                dermAllergy2.setAllergen(dermAllergens2[i]);
                dermAllergy2.setType("Medicamento");
                dermAllergy2.setSeverity("Leve");
                dermAllergy2.setReaction("Prurito leve");
                dermAllergy2.setActive(i != 3);
                allergyRepository.save(dermAllergy2);

                Medication dermMed1 = new Medication();
                dermMed1.setPatient(patient);
                dermMed1.setSpecialty("DERMATOLOGIA");
                dermMed1.setName(dermMeds1[i]);
                dermMed1.setDose("10 mg");
                dermMed1.setFrequency("1 vez al día");
                dermMed1.setStartDate(LocalDate.now().minusWeeks(2));
                medicationRepository.save(dermMed1);

                Medication dermMed2 = new Medication();
                dermMed2.setPatient(patient);
                dermMed2.setSpecialty("DERMATOLOGIA");
                dermMed2.setName(dermTreatments2[i]);
                dermMed2.setDose("Según indicación");
                dermMed2.setFrequency("Uso tópico");
                dermMed2.setStartDate(LocalDate.now().minusWeeks(1));
                dermMed2.setActive(i != 1);
                medicationRepository.save(dermMed2);

                // C3. Dermatología (nueva historia clínica)
                DermatologicalHistory dermHistory = new DermatologicalHistory();
                dermHistory.setPatient(patient);
                dermHistory.setSkinType(i % 2 == 0 ? "Sensible" : "Seca");
                dermHistory.setSunExposureHabits("Exposición solar moderada sin fotoprotección frecuente");
                dermHistory.setPersonalSkinHistory("Episodios previos de " + dermDiagnoses[i].toLowerCase() + ".");
                dermHistory.setFamilySkinHistory("Padre con psoriasis.");
                dermHistory.setChronicConditions(i % 2 == 0 ? "Rinitis alérgica" : "Asma leve");
                dermHistory.setExamFindings("Eritema leve, xerosis, sin signos de infección secundaria.");
                dermHistory.setNotes("Fototipo III.");
                dermatologicalHistoryRepository.save(dermHistory);

                Lesion lesion1 = new Lesion();
                lesion1.setPatient(patient);
                lesion1.setBodyArea(dermBodyAreas[i]);
                lesion1.setLesionType(dermLesionTypes[i]);
                lesion1.setSize("Placas de 2-3 cm");
                lesion1.setMorphology("Bordes bien definidos, superficie descamativa");
                lesion1.setColor("Eritematosa");
                lesion1.setSinceDate(LocalDate.now().minusMonths(2));
                lesion1.setEvolution("Estable en el último mes");
                lesionRepository.save(lesion1);

                Lesion lesion2 = new Lesion();
                lesion2.setPatient(patient);
                lesion2.setBodyArea("Cuello");
                lesion2.setLesionType("Mácula hiperpigmentada");
                lesion2.setSize("1 cm");
                lesion2.setColor("Marrón claro");
                lesion2.setSinceDate(LocalDate.now().minusWeeks(3));
                lesion2.setEvolution("Aparición reciente, sin cambios");
                lesionRepository.save(lesion2);

                Diagnosis dermDiag1 = new Diagnosis();
                dermDiag1.setPatient(patient);
                dermDiag1.setSpecialty("DERMATOLOGIA");
                dermDiag1.setCategory(dermCategories[i]);
                dermDiag1.setDescription(dermDiagnoses[i]);
                dermDiag1.setStatus("ACTIVO");
                dermDiag1.setDiagnosisDate(LocalDate.now().minusDays(3 + i));
                diagnosisRepository.save(dermDiag1);

                Diagnosis dermDiag2 = new Diagnosis();
                dermDiag2.setPatient(patient);
                dermDiag2.setSpecialty("DERMATOLOGIA");
                dermDiag2.setCategory("Otros hallazgos");
                dermDiag2.setDescription(dermSecondDiagnoses[i]);
                dermDiag2.setStatus(i % 2 == 0 ? "ACTIVO" : "RESUELTO");
                dermDiag2.setDiagnosisDate(LocalDate.now().minusMonths(1));
                diagnosisRepository.save(dermDiag2);

                AuxiliaryExam exam1 = new AuxiliaryExam();
                exam1.setPatient(patient);
                exam1.setExamType(dermExamTypes[i]);
                exam1.setDescription("Examen de lesiones cutáneas");
                exam1.setResult("Sin signos de malignidad");
                exam1.setExamDate(LocalDate.now().minusDays(3 + i));
                auxiliaryExamRepository.save(exam1);

                AuxiliaryExam exam2 = new AuxiliaryExam();
                exam2.setPatient(patient);
                exam2.setExamType("Examen de laboratorio");
                exam2.setDescription("Perfil general");
                exam2.setResult("Dentro de rangos normales");
                exam2.setExamDate(LocalDate.now().minusWeeks(2));
                auxiliaryExamRepository.save(exam2);

                Treatment treatment1 = new Treatment();
                treatment1.setPatient(patient);
                treatment1.setName(dermTreatments1[i]);
                treatment1.setDose("Según indicación");
                treatment1.setRoute("Tópica");
                treatment1.setFrequency("Diario");
                treatment1.setStartDate(LocalDate.now().minusWeeks(1));
                treatment1.setStatus("ACTIVO");
                treatmentRepository.save(treatment1);

                Treatment treatment2 = new Treatment();
                treatment2.setPatient(patient);
                treatment2.setName(dermTreatments2[i]);
                treatment2.setRoute("Tópica");
                treatment2.setFrequency("Mañana y noche");
                treatment2.setStartDate(LocalDate.now().minusDays(3));
                treatment2.setStatus("ACTIVO");
                treatmentRepository.save(treatment2);

                Procedure procedure1 = new Procedure();
                procedure1.setPatient(patient);
                procedure1.setName(dermProcedures1[i]);
                procedure1.setDescription("Procedimiento ambulatorio realizado sin complicaciones");
                procedure1.setProcedureDate(LocalDate.now().minusDays(3 + i));
                procedureRepository.save(procedure1);

                Procedure procedure2 = new Procedure();
                procedure2.setPatient(patient);
                procedure2.setName(dermProcedures2[i]);
                procedure2.setDescription("Procedimiento programado de control");
                procedure2.setProcedureDate(LocalDate.now().minusMonths(1));
                procedureRepository.save(procedure2);

                Evolution evolution1 = new Evolution();
                evolution1.setPatient(patient);
                evolution1.setControlDate(LocalDate.now().minusDays(1));
                evolution1.setClinicalNotes("Buena respuesta al tratamiento tópico. Se mantiene el plan.");
                evolution1.setNextControlDate(LocalDate.now().plusDays(28));
                evolutionRepository.save(evolution1);

                Evolution evolution2 = new Evolution();
                evolution2.setPatient(patient);
                evolution2.setControlDate(LocalDate.now().minusWeeks(4));
                evolution2.setClinicalNotes("Primer control: mejoría parcial de las lesiones.");
                evolution2.setNextControlDate(LocalDate.now().minusDays(1));
                evolutionRepository.save(evolution2);

                // D. Alerta de Riesgo - Dermatología
                RiskAlert dermAlert = new RiskAlert();
                dermAlert.setPatientId(patient.getId());
                dermAlert.setSpecialty("DERMATOLOGIA");
                dermAlert.setType("Alergia a corticosteroides");
                dermAlert.setLevel("Medio");
                dermAlert.setDescription("Vigilar reacción a corticoides tópicos.");
                dermAlert.setActive(true);
                riskAlertRepository.save(dermAlert);
            }

            // 6. Crear 10 Citas (5 de Psicología y 5 de Dermatología, divididas en distintos estados)
            String[] statuses = {"PROGRAMADA", "CONFIRMADA", "COMPLETADA", "CANCELADA", "NO_ASISTIO", "PROGRAMADA", "COMPLETADA", "CONFIRMADA", "CANCELADA", "COMPLETADA"};
            String[] modalities = {"PRESENCIAL", "VIRTUAL", "PRESENCIAL", "PRESENCIAL", "VIRTUAL", "VIRTUAL", "PRESENCIAL", "PRESENCIAL", "VIRTUAL", "PRESENCIAL"};
            for (int i = 0; i < 10; i++) {
                Appointment appointment = new Appointment();
                Patient assignedPatient;
                String appointmentSpecialty;
                int dayOffset;
                int timeHour;

                if (i < 5) {
                    // Citas de Psicología (pacientes 0 a 4)
                    assignedPatient = patients.get(i);
                    appointmentSpecialty = "PSICOLOGIA";
                    dayOffset = i - 2; // -2, -1, 0 (hoy), 1, 2
                    timeHour = 9 + (i * 2);
                } else {
                    // Citas de Dermatología (pacientes 5 a 9)
                    int dermIndex = i - 5;
                    assignedPatient = patients.get(i);
                    appointmentSpecialty = "DERMATOLOGIA";
                    dayOffset = dermIndex - 2; // -2, -1, 0 (hoy), 1, 2
                    timeHour = 9 + (dermIndex * 2);
                }

                appointment.setPatient(assignedPatient);
                appointment.setAppointmentDate(LocalDate.now().plusDays(dayOffset));
                appointment.setStartTime(LocalTime.of(timeHour, 0));
                appointment.setEndTime(LocalTime.of(timeHour + 1, 0));
                appointment.setStatus(statuses[i]);
                appointment.setModality(modalities[i]);
                appointment.setFirstTime(i % 3 == 0);
                appointment.setProfessionalId(1L);
                appointment.setNotes("Nota para cita de " + assignedPatient.getFirstName());
                appointment.setSpecialty(appointmentSpecialty);
                if ("VIRTUAL".equals(modalities[i])) {
                    appointment.setVideoCallLink("https://meet.google.com/abc-defg-hij");
                }
                appointmentRepository.save(appointment);
            }

            // 7. Crear Facturaciones con especialidad alineada al paciente
            for (int i = 0; i < patients.size(); i++) {
                Patient patient = patients.get(i);
                Payment payment = new Payment();
                payment.setPatient(patient);
                payment.setPaymentDate(LocalDateTime.now().minusDays(i % 5));
                payment.setPaymentMethod(i % 2 == 0 ? "EFECTIVO" : "TRANSFERENCIA");
                payment.setDescription("Facturación de servicios y/o insumos");
                String paymentSpecialty = patient.getSpecialty();
                payment.setSpecialty(paymentSpecialty);

                BigDecimal totalAmount = BigDecimal.ZERO;
                int numItems = (i % 2) + 1;

                for (int j = 0; j < numItems; j++) {
                    PaymentItem item = new PaymentItem();
                    item.setPayment(payment);
                    item.setQuantity(1);

                    if (j == 0) {
                        ClinicalService service = services.stream().filter(s -> s.getSpecialty().equals(paymentSpecialty)).findFirst().orElse(services.get(0));
                        item.setClinicalService(service);
                        item.setDescription("Servicio: " + service.getName());
                        item.setUnitPrice(service.getPrice());
                        item.setTotalPrice(service.getPrice());
                        totalAmount = totalAmount.add(item.getTotalPrice());
                    } else {
                        Supply supply = supplies.stream().filter(s -> s.getSpecialty().equals(paymentSpecialty)).findFirst().orElse(supplies.get(0));
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
