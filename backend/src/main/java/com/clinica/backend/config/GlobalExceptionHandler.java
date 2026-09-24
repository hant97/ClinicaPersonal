package com.clinica.backend.config;

import com.clinica.backend.dto.ApiErrorResponse;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.exception.BusinessRuleException;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.exception.StorageException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.query.sqm.UnknownPathException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Error de validación en los campos enviados")
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(Exception ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Usuario o contraseña incorrectos")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLockedException(LockedException ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Account Locked")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabledException(DisabledException ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Account Disabled")
                .message("La cuenta de usuario se encuentra deshabilitada")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("No tienes permisos suficientes para realizar esta acción")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFoundException(RuntimeException ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage() != null ? ex.getMessage() : "Recurso no encontrado")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Solicitud inválida";
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "No se pudo procesar la solicitud debido a un conflicto o restricción de datos");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
            fieldErrors.putIfAbsent(field, violation.getMessage());
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Error de validación en los campos enviados")
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // El mensaje del parser incluye clases Java y valores recibidos: queda en el log y al
    // cliente solo se le indica el campo afectado.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        log.warn("Solicitud no legible: {}", ex.getMessage());
        String field = ex.getCause() instanceof JacksonException jacksonException
                ? jacksonException.getPath().stream()
                        .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                        .collect(Collectors.joining("."))
                : "";
        return error(HttpStatus.BAD_REQUEST, field.isEmpty()
                ? "La solicitud contiene datos con un formato inválido"
                : "La solicitud contiene datos con un formato inválido en el campo '" + field + "'");
    }

    // Los listados aceptan `sort` de Spring Data; una propiedad inexistente es un error del cliente.
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSortProperty(PropertyReferenceException ex) {
        return error(HttpStatus.BAD_REQUEST, "Criterio de ordenación no válido: '" + ex.getPropertyName() + "'");
    }

    // En consultas JPQL (@Query) la propiedad inválida del `sort` la detecta Hibernate al ejecutar.
    // Las @Query se validan al arrancar, así que en ejecución esta causa solo proviene del `sort`.
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnknownPathException) {
                return error(HttpStatus.BAD_REQUEST, "Criterio de ordenación no válido");
            }
        }
        return handleGenericException(ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Tipo de argumento inválido: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un formato inválido");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.BAD_REQUEST, "El tamaño del archivo excede el límite máximo permitido");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no soportado para este endpoint");
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(StorageException ex) {
        log.error("Fallo de almacenamiento de archivos: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo procesar el archivo en este momento. Intente nuevamente más tarde.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Error inesperado en el servidor: ", ex);
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Ha ocurrido un error interno. Intente nuevamente más tarde.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
