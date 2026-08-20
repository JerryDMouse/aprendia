package com.aprendia.app.domain;

public final class ChatRecord {
    private final String question;
    private final String answer;
    private final String source;
    private final long createdAtMillis;

    public ChatRecord(String question, String answer, String source, long createdAtMillis) {
        this.question = question;
        this.answer = answer;
        this.source = source;
        this.createdAtMillis = createdAtMillis;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getSource() {
        return source;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
