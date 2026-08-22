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

        return answerFromEntry(question, entry, true);
    }

    public Answer answerWithoutLocalModel(String question) {
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

        return answerFromEntry(question, entry, false);
    }

    private Answer answerFromEntry(String question, KnowledgeEntry entry, boolean allowLocalModel) {
        String source = entry.getSubject() + ": " + entry.getTitle();
        if (allowLocalModel && localLlmEngine.isAvailable()) {
            try {
                String prompt = LlmPromptBuilder.build(question, entry);
                String generated = cleanGeneratedAnswer(localLlmEngine.generate(prompt));
                if (isCompleteGeneratedAnswer(generated)) {
                    return new Answer(generated, source + " + modelo local");
                }
                return new Answer(
                        "Intentemos aprenderlo juntos. " + entry.getContent(),
                        source
                );
            } catch (RuntimeException error) {
                return new Answer(
                        "Intentemos aprenderlo juntos. " + entry.getContent(),
                        source
                );
            }
        }

        return new Answer("Intentemos aprenderlo juntos. " + entry.getContent(), source);
    }

    private String cleanGeneratedAnswer(String generated) {
        String answer = generated == null ? "" : generated.trim();
        int nextRole = answer.indexOf("<|im_start|>user");
        if (nextRole >= 0) {
            answer = answer.substring(0, nextRole).trim();
        }
        answer = answer.replace("<|im_start|>assistant", "")
                .replace("<|im_start|>", "")
                .replace("<|im_end|>", "")
                .replace("<|endoftext|>", "")
                .trim();
        return answer;
    }

    private boolean isCompleteGeneratedAnswer(String answer) {
        if (answer == null || answer.length() < 20) {
            return false;
        }
        char lastChar = answer.charAt(answer.length() - 1);
        return lastChar == '.' || lastChar == '!' || lastChar == '?' || lastChar == '。';
    }

    private String shortError(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.length() > 80 ? message.substring(0, 80) : message;
    }
}
