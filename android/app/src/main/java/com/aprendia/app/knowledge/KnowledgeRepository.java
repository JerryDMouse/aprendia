package com.aprendia.app.knowledge;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class KnowledgeRepository {
    private final List<KnowledgeEntry> entries;

    public KnowledgeRepository(List<KnowledgeEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public int getEntryCount() {
        return entries.size();
    }

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