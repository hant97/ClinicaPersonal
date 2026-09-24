package com.clinica.backend.exception;

/**
 * Fallo del servidor al leer o escribir un archivo (disco o almacenamiento de objetos).
 * Se traduce a {@code 500}: no es un error del cliente, a diferencia de un archivo inválido.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
