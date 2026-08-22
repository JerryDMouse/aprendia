package com.aprendia.app.llm;

import com.aprendia.app.knowledge.KnowledgeEntry;

public final class LlmPromptBuilder {
    private LlmPromptBuilder() {
    }

    public static String build(String question, KnowledgeEntry entry) {
        String system = "Eres AprendIA. Responde en español para niños. "
                + "Usa solo el material. Da una respuesta completa en maximo 2 frases cortas.";

        String user = "Material: " + entry.getContent() + "\nPregunta: " + question;

        return "<|im_start|>system\n" + system + "<|im_end|>\n"
                + "<|im_start|>user\n" + user + "<|im_end|>\n"
                + "<|im_start|>assistant\n";
    }
}
