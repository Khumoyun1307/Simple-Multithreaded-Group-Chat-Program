package com.example.chat.common;

/**
 * Defines the kinds of messages exchanged.
 */
public enum MessageType {
    TEXT,   // Standard chat message
    JOIN,   // Client has joined
    LEAVE,  // Client has left
    PING,   // Heartbeat request
    PONG    // Heartbeat response
}
