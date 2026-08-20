package com.aprendia.app.domain;

import com.aprendia.app.knowledge.KnowledgeEntry;
import com.aprendia.app.knowledge.KnowledgeRepository;
import com.aprendia.app.safety.SafetyFilter;

public final class AnswerQuestionUseCase {
    private final KnowledgeRepository knowledgeRepository;
    private final SafetyFilter safetyFilter;

    public AnswerQuestionUseCase(KnowledgeRepository knowledgeRepository, SafetyFilter safetyFilter) {
        this.knowledgeRepository = knowledgeRepository;
        this.safetyFilter = safetyFilter;
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

        return new Answer(
                "Intentemos aprenderlo juntos. " + entry.getContent(),
                entry.getSubject() + ": " + entry.getTitle()
        );
    }
}
