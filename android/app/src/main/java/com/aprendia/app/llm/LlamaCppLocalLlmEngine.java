package com.aprendia.app.llm;

public final class LlamaCppLocalLlmEngine implements LocalLlmEngine {
    private final ModelFileStore modelFileStore;
    private final boolean nativeLibraryLoaded;

    public LlamaCppLocalLlmEngine(ModelFileStore modelFileStore) {
        this.modelFileStore = modelFileStore;
        nativeLibraryLoaded = loadNativeLibrary();
    }

    @Override
    public boolean isAvailable() {
        return nativeLibraryLoaded && modelFileStore.isModelInstalled();
    }

    @Override
    public String getStatus() {
        if (!modelFileStore.isModelInstalled()) {
            return modelFileStore.getInstallHint();
        }
        if (!nativeLibraryLoaded) {
            return "Motor llama.cpp local pendiente de empaquetar en la app.";
        }
        return "Modelo local listo: " + ModelFileStore.MODEL_DISPLAY_NAME;
    }

    @Override
    public String generate(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException(getStatus());
        }
        return generateNative(modelFileStore.getModelFile().getAbsolutePath(), prompt);
    }

    private boolean loadNativeLibrary() {
        try {
            System.loadLibrary("aprendia_llama");
            return true;
        } catch (UnsatisfiedLinkError error) {
            return false;
        }
    }

    private native String generateNative(String modelPath, String prompt);
}
