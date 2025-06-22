package com.example.chat.common;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a chat message on the wire.
 */
public class Message implements Serializable {
    private UUID id;
    private Instant timestamp;
    private String from;
    private String to;          // "ALL" for broadcast or a specific username
    private MessageType type;
    private String body;

    // No-arg constructor required by Gson
    public Message() { }

    // Convenient constructor for new outgoing messages
    public Message(String from, String to, MessageType type, String body) {
        this.id = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.from = from;
        this.to = to;
        this.type = type;
        this.body = body;
    }

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
