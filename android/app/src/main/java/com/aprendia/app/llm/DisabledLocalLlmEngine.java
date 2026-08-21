package com.aprendia.app.llm;

public final class DisabledLocalLlmEngine implements LocalLlmEngine {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getStatus() {
        return "Modelo local no instalado.";
    }

    @Override
    public String generate(String prompt) {
        throw new IllegalStateException(getStatus());
    }
}
