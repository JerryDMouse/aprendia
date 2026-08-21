package com.aprendia.app.domain;

import com.aprendia.app.knowledge.KnowledgeEntry;
import com.aprendia.app.knowledge.KnowledgeRepository;
import com.aprendia.app.llm.DisabledLocalLlmEngine;
import com.aprendia.app.llm.LlmPromptBuilder;
import com.aprendia.app.llm.LocalLlmEngine;
import com.aprendia.app.safety.SafetyFilter;

public final class AnswerQuestionUseCase {
    private final KnowledgeRepository knowledgeRepository;
    private final SafetyFilter safetyFilter;
    private final LocalLlmEngine localLlmEngine;

    public AnswerQuestionUseCase(KnowledgeRepository knowledgeRepository, SafetyFilter safetyFilter) {
        this(knowledgeRepository, safetyFilter, new DisabledLocalLlmEngine());
    }

    public AnswerQuestionUseCase(
            KnowledgeRepository knowledgeRepository,
            SafetyFilter safetyFilter,
            LocalLlmEngine localLlmEngine
    ) {
        this.knowledgeRepository = knowledgeRepository;
        this.safetyFilter = safetyFilter;
        this.localLlmEngine = localLlmEngine;
    }

    public Answer answer(String question) {
        if (safetyFilter.isUnsafe(question)) {
            return new Answer(
                    "No puedo ayudar con eso. Puedo acompanarte a estudiar o a resolver una duda escolar paso a paso.",
                    "Filtro de seguridad"
            );
        }

        KnowledgeEntry entry = knowledgeRepository.findBestEntry(question);
        if (entry == null) {
            return new Answer(
                    "No encontre esa informacion en tu material escolar.",
                    "Base de conocimiento local"
            );
        }

        String source = entry.getSubject() + ": " + entry.getTitle();
        if (localLlmEngine.isAvailable()) {
            try {
                String prompt = LlmPromptBuilder.build(question, entry);
                String generated = localLlmEngine.generate(prompt).trim();
                if (!generated.isEmpty()) {
                    return new Answer(generated, source + " + modelo local");
                }
            } catch (RuntimeException ignored) {
                // If local inference fails, keep the offline deterministic answer available.
            }
        }

        return new Answer("Intentemos aprenderlo juntos. " + entry.getContent(), source);
    }
}
