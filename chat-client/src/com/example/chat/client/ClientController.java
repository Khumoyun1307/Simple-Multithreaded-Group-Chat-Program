package com.example.chat.client;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages user interactions: sending JOIN, TEXT, LEAVE, responding to PING, and handling incoming messages.
 */
public class ClientController {
    private final ChatClient client;
    private final String username;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ClientController(ChatClient client, String username) {
        this.client = client;
        this.username = username;
    }

    public void start() throws Exception {
        client.connect();
        // Send JOIN message
        client.send(new Message(username, "ALL", MessageType.JOIN, ""));

        // Start a thread to handle incoming messages
        executor.execute(() -> {
            try {
                while (running.get()) {
                    Message msg = client.receive();
                    switch (msg.getType()) {
                        case PING:
                            // Respond with PONG to server
                            client.send(new Message(username, "SERVER", MessageType.PONG, ""));
                            break;
                        case PONG:
                            // Ignore server PONG responses
                            break;
                        default:
                            // Display JOIN, LEAVE, TEXT, etc.
                            System.out.println(msg.getFrom() + ": " + msg.getBody());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void sendChat(String body) {
        client.send(new Message(username, "ALL", MessageType.TEXT, body));
    }

    public void shutdown() {
        running.set(false);
        client.send(new Message(username, "ALL", MessageType.LEAVE, ""));
        client.disconnect();
        executor.shutdown();
    }
}