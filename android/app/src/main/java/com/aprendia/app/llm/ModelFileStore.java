package com.aprendia.app.llm;

import android.content.Context;

import java.io.File;

public final class ModelFileStore {
    public static final String MODEL_FILE_NAME = "qwen2.5-0.5b-instruct-q4.gguf";
    public static final String MODEL_DISPLAY_NAME = "Qwen2.5-0.5B-Instruct GGUF Q4";

    private final Context context;

    public ModelFileStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public File getModelFile() {
        File modelsDir = new File(context.getFilesDir(), "models");
        return new File(modelsDir, MODEL_FILE_NAME);
    }

    public boolean isModelInstalled() {
        File modelFile = getModelFile();
        return modelFile.exists() && modelFile.length() > 0;
    }

    public String getInstallHint() {
        return "Instala " + MODEL_DISPLAY_NAME + " en: " + getModelFile().getAbsolutePath();
    }
}
