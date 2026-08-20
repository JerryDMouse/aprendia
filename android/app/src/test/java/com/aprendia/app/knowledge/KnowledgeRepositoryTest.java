package com.aprendia.app.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

public final class KnowledgeRepositoryTest {
    private final KnowledgeRepository repository = new KnowledgeRepository(List.of(
            new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                    new String[]{"fotosintesis", "plantas", "sol", "hojas", "alimento"},
                    "La fotosintesis es el proceso por el cual las plantas fabrican su alimento. Usan la luz del sol, agua del suelo y aire. Las hojas ayudan a realizar este proceso."),
            new KnowledgeEntry("matematicas-suma", "Matematicas", "La suma",
                    new String[]{"suma", "sumar", "adicion", "juntar", "total"},
                    "La suma sirve para juntar cantidades y saber cuanto hay en total. Por ejemplo, si tienes 2 mangos y te dan 3 mas, ahora tienes 5 mangos."),
            new KnowledgeEntry("lenguaje-sustantivo", "Lenguaje", "El sustantivo",
                    new String[]{"sustantivo", "nombre", "persona", "animal", "cosa", "lugar"},
                    "Un sustantivo es una palabra que nombra personas, animales, lugares o cosas. Por ejemplo: nina, perro, escuela, rio y cuaderno."),
            new KnowledgeEntry("ambiental-agua", "Educacion ambiental", "Cuidado del agua",
                    new String[]{"agua", "cuidar", "rio", "quebrada", "ahorrar"},
                    "El agua se cuida cerrando la llave cuando no se usa, no botando basura en rios o quebradas y usando solo la cantidad necesaria para las actividades diarias.")
    ));

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