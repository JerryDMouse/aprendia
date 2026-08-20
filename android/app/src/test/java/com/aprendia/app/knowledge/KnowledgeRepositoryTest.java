package com.aprendia.app.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class KnowledgeRepositoryTest {
    private final KnowledgeRepository repository = new KnowledgeRepository();

    @Test
    public void findBestEntry_returnsPhotosynthesisEntry() {
        KnowledgeEntry entry = repository.findBestEntry("Que es la fotosintesis?");
        assertNotNull(entry);
        assertEquals("La fotosintesis", entry.getTitle());
    }

    @Test
    public void findBestEntry_returnsSumEntry() {
        KnowledgeEntry entry = repository.findBestEntry("Que es una suma?");
        assertNotNull(entry);
        assertEquals("La suma", entry.getTitle());
    }

    @Test
    public void findBestEntry_returnsNounEntry() {
        KnowledgeEntry entry = repository.findBestEntry("Que es un sustantivo?");
        assertNotNull(entry);
        assertEquals("El sustantivo", entry.getTitle());
    }

    @Test
    public void findBestEntry_returnsWaterEntry() {
        KnowledgeEntry entry = repository.findBestEntry("Como cuidar el agua?");
        assertNotNull(entry);
        assertEquals("Cuidado del agua", entry.getTitle());
    }

    @Test
    public void findBestEntry_normalizesAccentsAndCase() {
        KnowledgeEntry entry = repository.findBestEntry("¿Qué es la FOTOSÍNTESIS?");
        assertNotNull(entry);
        assertEquals("La fotosintesis", entry.getTitle());
    }

    @Test
    public void findBestEntry_returnsNullForUnknownTopic() {
        KnowledgeEntry entry = repository.findBestEntry("Que es un agujero negro?");
        assertNull(entry);
    }

    @Test
    public void findBestEntry_matchesLongestKeywordList() {
        KnowledgeEntry entry = repository.findBestEntry("La suma de un sustantivo en el rio");
        assertNotNull(entry);
    }
}