package com.clinica.backend.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Obtiene la IP del cliente sin leer cabeceras enviadas por él.
 * <p>
 * Detrás de un proxy, {@code server.forward-headers-strategy: native} activa el
 * {@code RemoteIpValve} de Tomcat, que solo acepta {@code X-Forwarded-For} cuando la
 * conexión llega desde un proxy de confianza y toma la primera IP no confiable de derecha a
 * izquierda. Leer la cabecera directamente permitiría falsificar la IP y eludir el bloqueo
 * por intentos fallidos, el límite de solicitudes públicas y la auditoría.
 */
public final class ClientIpResolver {

    public static final String UNKNOWN = "unknown";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : UNKNOWN;
    }
}
