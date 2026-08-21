package com.aprendia.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.aprendia.app.knowledge.KnowledgeEntry;
import com.aprendia.app.knowledge.KnowledgeRepository;
import com.aprendia.app.llm.LocalLlmEngine;
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

    @Test
    public void answer_usesLocalLlmOnlyWhenMaterialExists() {
        FakeLocalLlmEngine llm = new FakeLocalLlmEngine(true, "Respuesta generada con material escolar.");
        AnswerQuestionUseCase llmUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                        new String[]{"fotosintesis", "plantas", "sol", "hojas", "alimento"},
                        "La fotosintesis es el proceso por el cual las plantas fabrican su alimento.")
        )), new SafetyFilter(), llm);

        Answer answer = llmUseCase.answer("Que es la fotosintesis?");

        assertEquals("Respuesta generada con material escolar.", answer.getText());
        assertEquals("Ciencias naturales: La fotosintesis + modelo local", answer.getSource());
        assertTrue(llm.lastPrompt.contains("Usa únicamente el material escolar proporcionado."));
        assertTrue(llm.lastPrompt.contains("básica primaria"));
    }

    @Test
    public void answer_doesNotUseLocalLlmForUnknownTopic() {
        FakeLocalLlmEngine llm = new FakeLocalLlmEngine(true, "No debe usarse");
        AnswerQuestionUseCase llmUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                        new String[]{"fotosintesis"},
                        "La fotosintesis es el proceso por el cual las plantas fabrican su alimento.")
        )), new SafetyFilter(), llm);

        Answer answer = llmUseCase.answer("Que es un agujero negro?");

        assertTrue(answer.getText().contains("material escolar"));
        assertNull(llm.lastPrompt);
    }

    private static final class FakeLocalLlmEngine implements LocalLlmEngine {
        private final boolean available;
        private final String response;
        private String lastPrompt;

        private FakeLocalLlmEngine(boolean available, String response) {
            this.available = available;
            this.response = response;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String getStatus() {
            return available ? "Disponible" : "No instalado";
        }

        @Override
        public String generate(String prompt) {
            lastPrompt = prompt;
            return response;
        }
    }
}
