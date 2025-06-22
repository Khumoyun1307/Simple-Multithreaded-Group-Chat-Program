package com.example.chat.client.service;

import com.example.chat.client.ChatClient;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service layer that wraps ChatClient and exposes an ObservableList of messages.
 * Handles connection, JOIN/LEAVE, PING/PONG, and message dispatching.
 */
public class ChatService {
    private final ChatClient client;
    private final String username;
    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-service-listener");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ChatService(String host, int port, String username) throws IOException {
        this.client = new ChatClient(host, port);
        this.username = username;
    }

    /**
     * Starts the service: connects, sends JOIN, and begins listening.
     */
    public void start() throws IOException {
        client.connect();
        client.send(new Message(username, "ALL", MessageType.JOIN, ""));
        running.set(true);
        listenerExecutor.execute(this::receiveLoop);
    }

    private void receiveLoop() {
        try {
            while (running.get()) {
                Message msg = client.receive();
                if (msg == null) continue;
                switch (msg.getType()) {
                    case PING:
                        client.send(new Message(username, "SERVER", MessageType.PONG, ""));
                        break;
                    case PONG:
                        // ignore
                        break;
                    default:
                        String formatted = msg.getFrom() + ": " + msg.getBody();
                        Platform.runLater(() -> messages.add(formatted));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sends a text message to the server.
     */
    public void sendText(String text) {
        if (text == null || text.isEmpty()) return;
        client.send(new Message(username, "ALL", MessageType.TEXT, text));
    }

    /**
     * Stops the service: sends LEAVE, disconnects, and stops listener.
     */
    public void stop() {
        running.set(false);
        try {
            client.send(new Message(username, "ALL", MessageType.LEAVE, ""));
            client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        listenerExecutor.shutdownNow();
    }

    /**
     * Returns the observable list of formatted messages for UI binding.
     */
    public ObservableList<String> getMessages() {
        return messages;
    }
}
