package com.aprendia.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.aprendia.app.domain.ChatRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class HistoryStore {
    private static final String PREFS_NAME = "aprendia_history";
    private static final String HISTORY_KEY = "items";

    private final SharedPreferences preferences;

    public HistoryStore(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<ChatRecord> load() {
        String raw = preferences.getString(HISTORY_KEY, "[]");
        List<ChatRecord> records = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index += 1) {
                JSONObject object = array.getJSONObject(index);
                records.add(new ChatRecord(
                        object.optString("question"),
                        object.optString("answer"),
                        object.optString("source"),
                        object.optLong("createdAtMillis")
                ));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return records;
    }

    public void append(ChatRecord record) {
        List<ChatRecord> records = load();
        records.add(record);
        save(records);
    }

    public void clear() {
        preferences.edit().remove(HISTORY_KEY).apply();
    }

    private void save(List<ChatRecord> records) {
        JSONArray array = new JSONArray();
        for (ChatRecord record : records) {
            JSONObject object = new JSONObject();
            try {
                object.put("question", record.getQuestion());
                object.put("answer", record.getAnswer());
                object.put("source", record.getSource());
                object.put("createdAtMillis", record.getCreatedAtMillis());
                array.put(object);
            } catch (JSONException ignored) {
                // Skip malformed entries; all values originate inside the app.
            }
        }
        preferences.edit().putString(HISTORY_KEY, array.toString()).apply();
    }
}
