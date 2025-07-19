package com.example.chat.client.core;

import com.example.chat.common.Message;

/**
 * Listener for core chat events. UI layers implement this to receive updates.
 */
public interface ChatEventListener {
    /**
     * Called for any incoming Message (broadcast, private, or system messages).
     * @param msg the raw Message received from the server
     */
    void onMessage(Message msg);

    /**
     * Called when an encrypted message is received and successfully decrypted.
     * @param msg the original Message with encrypted payload
     * @param plaintext the decrypted text body
     */
    void onSecureMessage(Message msg, String plaintext);

    /**
     * Called when a user joins globally or a room (JOIN message).
     * @param msg the JOIN Message
     */
    void onUserJoined(Message msg);

    /**
     * Called when a user leaves globally or a room (LEAVE message).
     * @param msg the LEAVE Message
     */
    void onUserLeft(Message msg);

    /**
     * Called when a heartbeat PING is received.
     * @param msg the PING Message
     */
    void onPing(Message msg);

    /**
     * Called when a PONG response is received.
     * @param msg the PONG Message
     */
    void onPong(Message msg);

    /**
     * Called on any I/O or crypto error inside the core handler.
     * @param e the Exception thrown
     */
    void onError(Exception e);
}