package com.aprendia.app.safety;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class SafetyFilter {
    private final List<String> blockedTerms = List.of("trampa", "copiar", "arma", "violencia", "robar", "droga");

    public boolean isUnsafe(String question) {
        String normalized = normalize(question);
        for (String term : blockedTerms) {
            if (normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9ñ\\s]", " ").replaceAll("\\s+", " ").trim();
    }
}
