package com.aprendia.app.llm;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    public void importFrom(Uri uri) throws IOException {
        File modelFile = getModelFile();
        File modelsDir = modelFile.getParentFile();
        if (modelsDir != null && !modelsDir.exists() && !modelsDir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta del modelo.");
        }

        File tempFile = new File(modelFile.getAbsolutePath() + ".tmp");
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(tempFile)) {
            if (input == null) {
                throw new IOException("No se pudo abrir el archivo seleccionado.");
            }
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        if (modelFile.exists() && !modelFile.delete()) {
            throw new IOException("No se pudo reemplazar el modelo anterior.");
        }
        if (!tempFile.renameTo(modelFile)) {
            throw new IOException("No se pudo guardar el modelo importado.");
        }
    }
}
