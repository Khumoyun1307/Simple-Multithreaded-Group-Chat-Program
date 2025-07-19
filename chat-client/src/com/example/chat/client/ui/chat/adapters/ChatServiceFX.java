package com.example.chat.client.ui.chat.adapters;

import com.example.chat.client.core.ChatEventListener;
import com.example.chat.client.core.CoreChatHandler;
import com.example.chat.common.Message;
import com.cipherchat.engine.CryptoException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;

/**
 * JavaFX adapter for CoreChatHandler. Exposes an ObservableList for binding
 * and delegates UI actions to the core handler.
 */
public class ChatServiceFX implements ChatEventListener {
    private final CoreChatHandler core;
    private final ObservableList<String> messages = FXCollections.observableArrayList();

    public ChatServiceFX(String host, int port, String username) {
        this.core = new CoreChatHandler(host, port, username);
        this.core.addListener(this);
    }

    /**
     * Connect to the server and start receiving messages.
     */
    public void connect() throws IOException {
        core.connect();
    }

    /**
     * Disconnect cleanly.
     */
    public void disconnect() {
        core.disconnect();
    }

    /**
     * Send a plain text message.
     */
    public void sendText(String to, String body) {
        core.sendText(to, body);
    }

    /**
     * Send a secure encrypted message.
     */
    public void sendSecure(String to, String body) {
        try {
            core.sendSecure(to, body);
        } catch (CryptoException e) {
            Platform.runLater(() -> messages.add("[secure send failed] " + e.getMessage()));
        }
    }

    /**
     * Join a room.
     */
    public void joinRoom(String room) {
        core.joinRoom(room);
    }

    /**
     * Leave a room.
     */
    public void leaveRoom(String room) {
        core.leaveRoom(room);
    }

    /**
     * @return an observable list of display strings to bind to controls
     */
    public ObservableList<String> getMessages() {
        return messages;
    }

    // --- ChatEventListener implementations ---

    @Override
    public void onMessage(Message msg) {
        Platform.runLater(() -> messages.add(msg.getFrom() + ": " + msg.getBody()));
    }

    @Override
    public void onSecureMessage(Message msg, String plaintext) {
        Platform.runLater(() -> messages.add("[🔒] " + msg.getFrom() + ": " + plaintext));
    }

    @Override
    public void onUserJoined(Message msg) {
        String ctx = "ALL".equals(msg.getTo()) ? "the chat" : "room " + msg.getTo();
        Platform.runLater(() -> messages.add("* " + msg.getFrom() + " joined " + ctx + " *"));
    }

    @Override
    public void onUserLeft(Message msg) {
        String ctx = "ALL".equals(msg.getTo()) ? "the chat" : "room " + msg.getTo();
        Platform.runLater(() -> messages.add("* " + msg.getFrom() + " left " + ctx + " *"));
    }

    @Override
    public void onPing(Message msg) {
        // Core already auto-replies. Could log if desired.
    }

    @Override
    public void onPong(Message msg) {
        // Could update a heartbeat indicator in UI.
    }

    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> messages.add("[error] " + e.getMessage()));
    }

    public void sendSecureGroup(String text) {
        core.sendSecureGroup(text);
    }
}
