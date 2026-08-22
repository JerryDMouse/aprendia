package com.aprendia.app.llm;

import android.util.Log;

public final class LlamaCppLocalLlmEngine implements LocalLlmEngine {
    private static final String TAG = "AprendIA-LLM";

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
            return "Motor llama.cpp local no disponible en este dispositivo.";
        }
        return "Modelo local listo: " + ModelFileStore.MODEL_DISPLAY_NAME;
    }

    @Override
    public String generate(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException(getStatus());
        }
        long startedAt = System.currentTimeMillis();
        String modelPath = modelFileStore.getModelFile().getAbsolutePath();
        Log.i(TAG, "Starting local generation. modelBytes=" + modelFileStore.getModelFile().length()
                + " promptChars=" + prompt.length());
        String response = generateNative(modelPath, prompt);
        Log.i(TAG, "Finished local generation in " + (System.currentTimeMillis() - startedAt)
                + " ms, responseChars=" + (response == null ? 0 : response.length()));
        return response;
    }

    public void preload() {
        if (!isAvailable()) {
            return;
        }
        long startedAt = System.currentTimeMillis();
        Log.i(TAG, "Preloading local model. modelBytes=" + modelFileStore.getModelFile().length());
        loadModelNative(modelFileStore.getModelFile().getAbsolutePath());
        Log.i(TAG, "Preloaded local model in " + (System.currentTimeMillis() - startedAt) + " ms");
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

    private native void loadModelNative(String modelPath);
}
