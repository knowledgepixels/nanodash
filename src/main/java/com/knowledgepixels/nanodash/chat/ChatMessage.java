package com.knowledgepixels.nanodash.chat;

import java.io.Serializable;

/**
 * One entry in a Claude chat conversation as shown in the chat panel.
 */
public class ChatMessage implements Serializable {

    /**
     * The kind of chat entry, determining how it is rendered.
     */
    public enum Kind {
        USER, ASSISTANT, TOOL, ERROR
    }

    private final Kind kind;
    private final String text;
    private final long timestamp;

    ChatMessage(Kind kind, String text) {
        this.kind = kind;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public Kind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
