package com.example.chat.client.core;

import com.example.chat.common.GsonFactory;
import com.example.chat.common.Message;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Low-level client handling socket I/O and JSON (de)serialization.
 */
public class ChatClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final Gson gson = GsonFactory.getGson();
    private final BlockingQueue<Message> inbound = new LinkedBlockingQueue<>();

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects to the server and starts listener thread.
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        // Start a thread to read incoming messages
        new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    Message msg = gson.fromJson(line, Message.class);
                    inbound.offer(msg);
                }
            } catch (IOException e) {
                // Connection lost
            }
        }, "client-listener").start();
    }

    /**
     * Sends a message to the server.
     */
    public void send(Message message) {
        try {
            String json = gson.toJson(message);
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            // Handle send error
        }
    }

    /**
     * Retrieves the next inbound message, blocking if none are available.
     */
    public Message receive() throws InterruptedException {
        return inbound.take();
    }

    /**
     * Closes the connection.
     */
    public void disconnect() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}