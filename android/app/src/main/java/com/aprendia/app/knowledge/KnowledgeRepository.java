package com.aprendia.app.knowledge;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class KnowledgeRepository {
    private final List<KnowledgeEntry> entries = List.of(
            new KnowledgeEntry(
                    "Ciencias naturales",
                    "La fotosintesis",
                    new String[] {"fotosintesis", "plantas", "sol", "hojas", "alimento"},
                    "La fotosintesis es el proceso por el cual las plantas fabrican su alimento. Usan la luz del sol, agua del suelo y aire. Las hojas ayudan a realizar este proceso."
            ),
            new KnowledgeEntry(
                    "Matematicas",
                    "La suma",
                    new String[] {"suma", "sumar", "adicion", "juntar", "total"},
                    "La suma sirve para juntar cantidades y saber cuanto hay en total. Por ejemplo, si tienes 2 mangos y te dan 3 mas, ahora tienes 5 mangos."
            ),
            new KnowledgeEntry(
                    "Lenguaje",
                    "El sustantivo",
                    new String[] {"sustantivo", "nombre", "persona", "animal", "cosa", "lugar"},
                    "Un sustantivo es una palabra que nombra personas, animales, lugares o cosas. Por ejemplo: nina, perro, escuela, rio y cuaderno."
            ),
            new KnowledgeEntry(
                    "Educacion ambiental",
                    "Cuidado del agua",
                    new String[] {"agua", "cuidar", "rio", "quebrada", "ahorrar"},
                    "El agua se cuida cerrando la llave cuando no se usa, no botando basura en rios o quebradas y usando solo la cantidad necesaria para las actividades diarias."
            )
    );

    public KnowledgeEntry findBestEntry(String question) {
        String normalizedQuestion = normalize(question);
        List<ScoredEntry> matches = new ArrayList<>();
        for (KnowledgeEntry entry : entries) {
            int score = score(normalizedQuestion, entry);
            if (score > 0) {
                matches.add(new ScoredEntry(entry, score));
            }
        }
        return matches.stream()
                .max(Comparator.comparingInt(ScoredEntry::getScore))
                .map(ScoredEntry::getEntry)
                .orElse(null);
    }

    private int score(String normalizedQuestion, KnowledgeEntry entry) {
        int result = 0;
        for (String keyword : entry.getKeywords()) {
            if (normalizedQuestion.contains(normalize(keyword))) {
                result += 1;
            }
        }
        return result;
    }

    private String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9ñ\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private static final class ScoredEntry {
        private final KnowledgeEntry entry;
        private final int score;

        private ScoredEntry(KnowledgeEntry entry, int score) {
            this.entry = entry;
            this.score = score;
        }

        private KnowledgeEntry getEntry() {
            return entry;
        }

        private int getScore() {
            return score;
        }
    }
}
