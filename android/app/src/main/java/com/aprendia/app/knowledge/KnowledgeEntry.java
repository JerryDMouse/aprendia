package com.aprendia.app.knowledge;

public final class KnowledgeEntry {
    private final String id;
    private final String subject;
    private final String title;
    private final String[] keywords;
    private final String content;

    public KnowledgeEntry(String id, String subject, String title, String[] keywords, String content) {
        this.id = id;
        this.subject = subject;
        this.title = title;
        this.keywords = keywords.clone();
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getTitle() {
        return title;
    }

    public String[] getKeywords() {
        return keywords.clone();
    }

    public String getContent() {
        return content;
    }
}
