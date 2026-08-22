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
        assertTrue(llm.lastPrompt.contains("Usa solo el material."));
        assertTrue(llm.lastPrompt.contains("respuesta completa"));
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

    @Test
    public void answer_cleansLocalLlmChatTemplateTokens() {
        FakeLocalLlmEngine llm = new FakeLocalLlmEngine(true,
                "<|im_start|>assistant\nLas plantas fabrican su alimento con luz, agua y aire.<|im_end|>");
        AnswerQuestionUseCase llmUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                        new String[]{"fotosintesis", "plantas"},
                        "La fotosintesis es el proceso por el cual las plantas fabrican su alimento.")
        )), new SafetyFilter(), llm);

        Answer answer = llmUseCase.answer("Que es la fotosintesis?");

        assertEquals("Las plantas fabrican su alimento con luz, agua y aire.", answer.getText());
    }

    @Test
    public void answer_fallsBackToKnowledgeWhenLocalLlmFails() {
        FakeLocalLlmEngine llm = new FakeLocalLlmEngine(true, "Respuesta");
        llm.failure = new IllegalStateException("No se pudo iniciar el contexto del modelo.");
        AnswerQuestionUseCase llmUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                        new String[]{"fotosintesis", "plantas"},
                        "La fotosintesis es el proceso por el cual las plantas fabrican su alimento.")
        )), new SafetyFilter(), llm);

        Answer answer = llmUseCase.answer("Que es la fotosintesis?");

        assertEquals("Ciencias naturales: La fotosintesis", answer.getSource());
        assertTrue(answer.getText().contains("Intentemos aprenderlo juntos"));
    }

    @Test
    public void answer_fallsBackToKnowledgeWhenLocalLlmResponseIsEmpty() {
        FakeLocalLlmEngine llm = new FakeLocalLlmEngine(true, "   ");
        AnswerQuestionUseCase llmUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                        new String[]{"fotosintesis", "plantas"},
                        "La fotosintesis es el proceso por el cual las plantas fabrican su alimento.")
        )), new SafetyFilter(), llm);

        Answer answer = llmUseCase.answer("Que es la fotosintesis?");

        assertEquals("Ciencias naturales: La fotosintesis", answer.getSource());
        assertTrue(answer.getText().contains("Intentemos aprenderlo juntos"));
    }

    @Test
    public void answer_fallsBackToKnowledgeWhenLocalLlmResponseIsIncomplete() {
        FakeLocalLlmEngine llm = new FakeLocalLlmEngine(true, "Los gatos pertenecen a la especie");
        AnswerQuestionUseCase llmUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                new KnowledgeEntry("ciencias-gatos", "Ciencias naturales", "Gatos domesticos",
                        new String[]{"gatos", "especie", "felis catus"},
                        "Los gatos domésticos son animales mamíferos y felinos. Su especie se llama Felis catus.")
        )), new SafetyFilter(), llm);

        Answer answer = llmUseCase.answer("A que especie pertenecen los gatos?");

        assertEquals("Ciencias naturales: Gatos domesticos", answer.getSource());
        assertTrue(answer.getText().contains("Felis catus"));
    }

    @Test
    public void answerWithoutLocalModel_skipsAvailableLocalLlm() {
        FakeLocalLlmEngine llm = new FakeLocalLlmEngine(true, "No debe usarse");
        AnswerQuestionUseCase llmUseCase = new AnswerQuestionUseCase(new KnowledgeRepository(List.of(
                new KnowledgeEntry("ciencias-fotosintesis", "Ciencias naturales", "La fotosintesis",
                        new String[]{"fotosintesis", "plantas"},
                        "La fotosintesis es el proceso por el cual las plantas fabrican su alimento.")
        )), new SafetyFilter(), llm);

        Answer answer = llmUseCase.answerWithoutLocalModel("Que es la fotosintesis?");

        assertTrue(answer.getText().contains("Intentemos aprenderlo juntos"));
        assertEquals("Ciencias naturales: La fotosintesis", answer.getSource());
        assertNull(llm.lastPrompt);
    }

    private static final class FakeLocalLlmEngine implements LocalLlmEngine {
        private final boolean available;
        private final String response;
        private String lastPrompt;
        private RuntimeException failure;

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
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
