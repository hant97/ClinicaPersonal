package com.clinica.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PaginationValidationInterceptor implements HandlerInterceptor {

    static final int MAX_PAGE_SIZE = 100;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        validateIntegerParameter(request, "page", 0, Integer.MAX_VALUE);
        validateIntegerParameter(request, "size", 1, MAX_PAGE_SIZE);
        return true;
    }

    private void validateIntegerParameter(HttpServletRequest request, String name, int minimum, int maximum) {
        String rawValue = request.getParameter(name);
        if (rawValue == null) {
            return;
        }

        final int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El parámetro '" + name + "' debe ser un número entero");
        }

        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    "El parámetro '" + name + "' debe estar entre " + minimum + " y " + maximum
            );
        }
    }
}
