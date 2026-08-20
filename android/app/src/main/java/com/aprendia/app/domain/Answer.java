package com.aprendia.app.domain;

public final class Answer {
    private final String text;
    private final String source;

    public Answer(String text, String source) {
        this.text = text;
        this.source = source;
    }

    public String getText() {
        return text;
    }

    public String getSource() {
        return source;
    }
}
