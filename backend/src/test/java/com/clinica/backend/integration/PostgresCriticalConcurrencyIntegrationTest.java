package com.clinica.backend.integration;

import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Supply;
import com.clinica.backend.model.TransactionReason;
import com.clinica.backend.model.TransactionType;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.InventoryTransactionRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.SupplyRepository;
import com.clinica.backend.service.AppointmentService;
import com.clinica.backend.service.InventoryTransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
class PostgresCriticalConcurrencyIntegrationTest {

    @Autowired
    private InventoryTransactionService inventoryTransactionService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired
    private SupplyRepository supplyRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private PatientRepository patientRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("POSTGRES_TEST_URL"));
        registry.add("spring.datasource.username",
                () -> System.getenv().getOrDefault("POSTGRES_TEST_USER", "postgres"));
        registry.add("spring.datasource.password",
                () -> System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", "postgres"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @AfterEach
    void cleanUp() {
        inventoryTransactionRepository.deleteAll();
        appointmentRepository.deleteAll();
        supplyRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @Test
    void onlyOneConcurrentOutputSucceedsWhenStockIsOne() throws Exception {
        Supply supply = new Supply();
        supply.setName("Guantes concurrentes");
        supply.setCurrentStock(1);
        supply.setMinStockLevel(0);
        supply.setSpecialty("PSICOLOGIA");
        supply = supplyRepository.save(supply);

        Long supplyId = supply.getId();
        Callable<Boolean> operation = authenticatedOperation(() -> {
            InventoryTransactionDto request = new InventoryTransactionDto();
            request.setSupplyId(supplyId);
            request.setQuantity(1);
            request.setType(TransactionType.OUT);
            request.setReason(TransactionReason.CLINICAL_USAGE);
            try {
                inventoryTransactionService.recordTransaction(request);
                return true;
            } catch (ConflictException exception) {
                return false;
            }
        });

        int successes = runConcurrently(operation, operation);

        assertEquals(1, successes);
        assertEquals(0, supplyRepository.findById(supplyId).orElseThrow().getCurrentStock());
        assertEquals(1, inventoryTransactionRepository.count());
    }

    @Test
    void onlyOneConcurrentAppointmentSucceedsForTheSameInterval() throws Exception {
        Patient patient = new Patient();
        patient.setFirstName("Paciente");
        patient.setLastName("Concurrente");
        patient.setGender("OTRO");
        patient.setSpecialty("PSICOLOGIA");
        patient = patientRepository.save(patient);

        Long patientId = patient.getId();
        Callable<Boolean> operation = authenticatedOperation(() -> {
            AppointmentDto request = new AppointmentDto();
            request.setPatientId(patientId);
            request.setAppointmentDate(LocalDate.of(2030, 9, 1));
            request.setStartTime(LocalTime.of(10, 0));
            request.setEndTime(LocalTime.of(11, 0));
            request.setStatus("PROGRAMADA");
            try {
                appointmentService.create(request);
                return true;
            } catch (ConflictException exception) {
                return false;
            }
        });

        int successes = runConcurrently(operation, operation);

        assertEquals(1, successes);
        assertEquals(1, appointmentRepository.count());
    }

    private Callable<Boolean> authenticatedOperation(Callable<Boolean> operation) {
        return () -> {
            User user = new User();
            user.setId(999L);
            user.setUsername("concurrency.test");
            user.setSpecialty("PSICOLOGIA");
            user.setRoles(Set.of("ROLE_ADMIN"));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            try {
                return operation.call();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private int runConcurrently(Callable<Boolean> firstOperation, Callable<Boolean> secondOperation)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = synchronizedOperation(firstOperation, ready, start);
            Callable<Boolean> second = synchronizedOperation(secondOperation, ready, start);
            Future<Boolean> firstResult = executor.submit(first);
            Future<Boolean> secondResult = executor.submit(second);
            ready.await();
            start.countDown();
            return (firstResult.get() ? 1 : 0) + (secondResult.get() ? 1 : 0);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> synchronizedOperation(
            Callable<Boolean> operation,
            CountDownLatch ready,
            CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return operation.call();
        };
    }
}
