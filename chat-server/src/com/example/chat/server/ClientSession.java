package com.example.chat.server;

import com.example.chat.common.GsonFactory;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Handles communication with a single client.
 * Responds to PINGs automatically with PONG.
 */
public class ClientSession implements Runnable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Gson gson = GsonFactory.getGson();
    private String username;

    public ClientSession(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        // Read initial JOIN message
        String joinJson = reader.readLine();
        Message joinMsg = gson.fromJson(joinJson, Message.class);
        if (joinMsg.getType() == MessageType.JOIN) {
            this.username = joinMsg.getFrom();
            ChatManager.getInstance().addSession(username, this);
        } else {
            throw new IllegalStateException("First message must be JOIN");
        }
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Message msg = gson.fromJson(line, Message.class);
                switch (msg.getType()) {
                    case TEXT -> ChatManager.getInstance().broadcast(msg);
                    case PING -> sendMessage(new Message("SERVER", username, MessageType.PONG, "PONG"));
                    case SECURE_TEXT -> {
                        // Send only to intended recipient
                        boolean success = ChatManager.getInstance().sendTo(msg.getTo(), msg);
                        if (!success) {
                            System.err.println("Failed to route SECURE_TEXT to " + msg.getTo());
                        }
                    }
                    default -> {
                        // Other message types can be handled here
                    }
                }
            }
        } catch (IOException e) {
            // Connection lost or error
        } finally {
            cleanup();
        }
    }

    /**
     * Sends a Message object to the client as JSON.
     */
    public void sendMessage(Message message) {
        try {
            String json = gson.toJson(message);
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            socket.close();
        } catch (IOException ignored) {}
        ChatManager.getInstance().removeSession(username);
    }
}