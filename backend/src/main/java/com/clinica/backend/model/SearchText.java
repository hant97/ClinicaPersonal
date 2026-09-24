package com.clinica.backend.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza texto para búsquedas sin distinción de mayúsculas ni tildes: "José Núñez" y
 * "jose nunez" coinciden. La columna {@code patients.search_text} se guarda con esta forma y la
 * consulta se normaliza igual antes de comparar.
 */
public final class SearchText {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private SearchText() {
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String withoutAccents = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }

    public static String of(String... parts) {
        StringBuilder text = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!text.isEmpty()) text.append(' ');
                text.append(part.trim());
            }
        }
        return normalize(text.toString());
    }
}
