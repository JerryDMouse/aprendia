package com.aprendia.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.aprendia.app.knowledge.KnowledgeEntry;
import com.aprendia.app.knowledge.KnowledgeRepository;
import com.aprendia.app.safety.SafetyFilter;

import java.util.List;

import org.junit.Test;

public final class AnswerQuestionUseCaseTest {
    private final AnswerQuestionUseCase useCase =
            new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                    new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                            new String[]{"fotosintesis", "plantas", "sol", "hojas", "alimento"},
                            "La fotosintesis es el proceso por el cual las plantas fabrican su alimento. Usan la luz del sol, agua del suelo y aire. Las hojas ayudan a realizar este proceso.")
            )), new SafetyFilter());

    @Test
    public void answer_returnsKnowledgeForKnownTopic() {
        Answer answer = useCase.answer("Que es la fotosintesis?");
        assertNotNull(answer);
        assertTrue(answer.getText().contains("fotosintesis"));
        assertEquals("Ciencias naturales: La fotosintesis", answer.getSource());
    }

    @Test
    public void answer_returnsSafeFallbackForUnknownTopic() {
        Answer answer = useCase.answer("Que es un agujero negro?");
        assertNotNull(answer);
        assertTrue(answer.getText().toLowerCase().contains("no encontre"));
        assertEquals("Base de conocimiento local", answer.getSource());
    }

    @Test
    public void answer_returnsSecurityResponseForBlockedTopic() {
        Answer answer = useCase.answer("Como hago trampa en un examen?");
        assertNotNull(answer);
        assertEquals("Filtro de seguridad", answer.getSource());
    }

    @Test
    public void answer_neverLeaksUnknownMaterial() {
        Answer answer = useCase.answer("Que es un agujero negro?");
        assertTrue(answer.getText().contains("material escolar"));
    }
}