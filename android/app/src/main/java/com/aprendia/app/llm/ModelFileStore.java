package com.aprendia.app.llm;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ModelFileStore {
    public static final String MODEL_FILE_NAME = "qwen2.5-0.5b-instruct-q2_k.gguf";
    public static final String MODEL_DISPLAY_NAME = "Qwen2.5-0.5B-Instruct GGUF Q2_K";
    private static final String MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q2_k.gguf?download=true";
    private static final String MODEL_VERSION = "qwen2.5-0.5b-instruct-q2_k:9217f5db79a29953eb74d5343926648285ec7e67";
    private static final long MIN_MODEL_BYTES = 100L * 1024L * 1024L;

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
        return modelFile.exists() && modelFile.length() >= MIN_MODEL_BYTES;
    }

    public boolean shouldDownloadDefaultModel() {
        return !isModelInstalled() || !MODEL_VERSION.equals(readInstalledModelVersion());
    }

    public String getInstallHint() {
        return "Descargando " + MODEL_DISPLAY_NAME + " en segundo plano.";
    }

    public void downloadDefaultModel(DownloadProgressListener listener) throws IOException {
        File modelFile = getModelFile();
        File modelsDir = modelFile.getParentFile();
        if (modelsDir != null && !modelsDir.exists() && !modelsDir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta del modelo.");
        }

        File tempFile = new File(modelFile.getAbsolutePath() + ".tmp");
        if (tempFile.exists() && !tempFile.delete()) {
            throw new IOException("No se pudo limpiar una descarga anterior.");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);

        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            connection.disconnect();
            throw new IOException("No se pudo descargar el modelo. Codigo HTTP: " + statusCode);
        }

        long totalBytes = connection.getContentLengthLong();
        long copiedBytes = 0;
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                copiedBytes += read;
                listener.onProgress(copiedBytes, totalBytes);
            }
        } finally {
            connection.disconnect();
        }

        if (tempFile.length() < MIN_MODEL_BYTES) {
            deleteTempFile(tempFile);
            throw new IOException("La descarga del modelo quedo incompleta.");
        }

        replaceModel(tempFile, modelFile);
        writeInstalledModelVersion();
    }

    private File getModelVersionFile() {
        return new File(getModelFile().getAbsolutePath() + ".version");
    }

    private String readInstalledModelVersion() {
        File versionFile = getModelVersionFile();
        if (!versionFile.exists()) {
            return "";
        }
        try (FileInputStream input = new FileInputStream(versionFile)) {
            byte[] bytes = new byte[(int) versionFile.length()];
            int read = input.read(bytes);
            if (read <= 0) {
                return "";
            }
            return new String(bytes, 0, read, StandardCharsets.UTF_8).trim();
        } catch (IOException error) {
            return "";
        }
    }

    private void writeInstalledModelVersion() throws IOException {
        try (FileOutputStream output = new FileOutputStream(getModelVersionFile())) {
            output.write(MODEL_VERSION.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void replaceModel(File tempFile, File modelFile) throws IOException {
        if (modelFile.exists() && !modelFile.delete()) {
            deleteTempFile(tempFile);
            throw new IOException("No se pudo reemplazar el modelo anterior.");
        }
        if (!tempFile.renameTo(modelFile)) {
            deleteTempFile(tempFile);
            throw new IOException("No se pudo guardar el modelo descargado.");
        }
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile.exists() && !tempFile.delete()) {
            tempFile.deleteOnExit();
        }
    }

    public interface DownloadProgressListener {
        void onProgress(long copiedBytes, long totalBytes);
    }
}
