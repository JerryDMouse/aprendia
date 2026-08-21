package com.aprendia.app.llm;

public interface LocalLlmEngine {
    boolean isAvailable();

    String getStatus();

    String generate(String prompt);
}
