package com.aprendia.app.knowledge;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class KnowledgeAssetsLoader {
    private static final String TAG = "KnowledgeAssetsLoader";
    private static final String KNOWLEDGE_DIR = "knowledge";

    private KnowledgeAssetsLoader() {
    }

    public static List<KnowledgeEntry> load(Context context) {
        List<KnowledgeEntry> entries = new ArrayList<>();
        try {
            String[] files = context.getAssets().list(KNOWLEDGE_DIR);
            if (files == null) {
                return entries;
            }
            for (String fileName : files) {
                if (!fileName.endsWith(".json")) {
                    continue;
                }
                entries.addAll(parseFile(context, KNOWLEDGE_DIR + "/" + fileName));
            }
        } catch (IOException e) {
            Log.e(TAG, "No se pudo cargar la base de conocimiento", e);
        }
        return entries;
    }

    private static List<KnowledgeEntry> parseFile(Context context, String assetPath) throws IOException {
        List<KnowledgeEntry> entries = new ArrayList<>();
        String raw;
        try (InputStream input = context.getAssets().open(assetPath)) {
            raw = new String(readFully(input), StandardCharsets.UTF_8);
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index += 1) {
                JSONObject object = array.getJSONObject(index);
                entries.add(parseEntry(object));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON invalido en " + assetPath, e);
        }
        return entries;
    }

    private static byte[] readFully(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static KnowledgeEntry parseEntry(JSONObject object) throws JSONException {
        JSONArray rawKeywords = object.getJSONArray("keywords");
        List<String> keywords = new ArrayList<>();
        for (int index = 0; index < rawKeywords.length(); index += 1) {
            keywords.add(rawKeywords.getString(index));
        }
        return new KnowledgeEntry(
                object.getString("id"),
                object.getString("subject"),
                object.getString("title"),
                keywords.toArray(new String[0]),
                object.getString("content")
        );
    }
}