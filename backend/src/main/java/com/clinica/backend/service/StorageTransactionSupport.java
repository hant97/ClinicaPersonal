package com.clinica.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

/** Coordinates blob cleanup with the completion of the database transaction. */
final class StorageTransactionSupport {

    private static final Logger log = LoggerFactory.getLogger(StorageTransactionSupport.class);

    private StorageTransactionSupport() {
    }

    static void deleteAfterCommit(Consumer<String> delete, String key, String description) {
        registerCleanup(delete, key, null, description);
    }

    static void deleteAfterRollback(Consumer<String> delete, String key, String description) {
        registerCleanup(delete, null, key, description);
    }

    private static void registerCleanup(
            Consumer<String> delete,
            String keyAfterCommit,
            String keyAfterRollback,
            String description
    ) {
        if (isBlank(keyAfterCommit) && isBlank(keyAfterRollback)) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(delete, keyAfterCommit, description);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String key = switch (status) {
                    case STATUS_COMMITTED -> keyAfterCommit;
                    case STATUS_ROLLED_BACK -> keyAfterRollback;
                    default -> null;
                };
                deleteQuietly(delete, key, description);
            }
        });
    }

    private static void deleteQuietly(Consumer<String> delete, String key, String description) {
        if (isBlank(key)) {
            return;
        }
        try {
            delete.accept(key);
        } catch (RuntimeException exception) {
            log.warn("No se pudo limpiar {} {} tras finalizar la transacción: {}", description, key, exception.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
