package com.aprendia.app.llm;

import com.aprendia.app.knowledge.KnowledgeEntry;

public final class LlmPromptBuilder {
    private LlmPromptBuilder() {
    }

    public static String build(String question, KnowledgeEntry entry) {
        return "Eres AprendIA, un acompañante educativo para niños de 6 a 10 años.\n"
                + "Responde solo sobre educación de básica primaria.\n"
                + "Usa únicamente el material escolar proporcionado.\n"
                + "No inventes información, no salgas del tema y no respondas temas no educativos.\n"
                + "Si falta información, di: No encontré esa información en mi material escolar.\n"
                + "Responde en español claro, corto y amable.\n\n"
                + "Materia: " + entry.getSubject() + "\n"
                + "Tema: " + entry.getTitle() + "\n"
                + "Material escolar:\n" + entry.getContent() + "\n\n"
                + "Pregunta del niño:\n" + question + "\n\n"
                + "Respuesta:";
    }
}
