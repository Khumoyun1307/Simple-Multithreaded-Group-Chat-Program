package com.example.chat.client.ui;

import com.example.chat.client.core.ChatEventListener;
import com.example.chat.client.core.CoreChatHandler;
import com.example.chat.common.Config;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.cipherchat.engine.CryptoException;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Refactored console application using CoreChatHandler as the chat engine.
 */
public class ConsoleApp implements ChatEventListener {
    private final CoreChatHandler core;
    private final Scanner scanner = new Scanner(System.in);
    private final String host;
    private final int port;
    private final String username;
    private volatile boolean running = true;

    public ConsoleApp(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.core = new CoreChatHandler(host, port, username);
        this.core.addListener(this);
    }

    public void start() throws IOException {
        System.out.println("Connecting to " + host + ":" + port);
        core.connect();
        System.out.println("Connected as " + username + "! Type messages or '/quit' to exit.");

        while (running) {
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("/quit")) {
                shutdown();
                break;
            } else if (line.startsWith("/secure ")) {
                String[] parts = line.split("\\s+", 3);
                if (parts.length < 3) {
                    System.out.println("Usage: /secure <username> <message>");
                } else {
                    try {
                        core.sendSecure(parts[1], parts[2]);
                    } catch (CryptoException e) {
                        System.err.println("Encryption failed: " + e.getMessage());
                    }
                }
            } else if (line.startsWith("/join ")) {
                String room = line.substring(6).trim();
                core.joinRoom(room);
            } else if (line.startsWith("/leave ")) {
                String room = line.substring(7).trim();
                core.leaveRoom(room);
            } else if (line.startsWith("/rooms")) {
                try {
                    List<String> rooms = core.listRooms();
                    System.out.println("Available rooms: " + rooms);
                } catch (UnsupportedOperationException e) {
                    System.out.println("Room listing not supported by server.");
                }
            } else {
                // default to broadcast
                core.sendText("ALL", line);
            }
        }
    }

    public void shutdown() {
        running = false;
        core.disconnect();
        System.out.println("Disconnected.");
    }

    // --- ChatEventListener implementations ---

    @Override
    public void onMessage(Message msg) {
        System.out.println(msg.getFrom() + ": " + msg.getBody());
    }

    @Override
    public void onSecureMessage(Message msg, String plaintext) {
        System.out.println("[🔒] " + msg.getFrom() + ": " + plaintext);
    }

    @Override
    public void onUserJoined(Message msg) {
        if (msg.getType() == MessageType.JOIN) {
            String target = msg.getTo();
            String joinContext = "ALL".equals(target) ? "the chat" : "room " + target;
            System.out.println("* " + msg.getFrom() + " has joined " + joinContext + " *");
        }
    }

    @Override
    public void onUserLeft(Message msg) {
        if (msg.getType() == MessageType.LEAVE) {
            String target = msg.getTo();
            String leaveContext = "ALL".equals(target) ? "the chat" : "room " + target;
            System.out.println("* " + msg.getFrom() + " has left " + leaveContext + " *");
        }
    }

    @Override
    public void onPing(Message msg) {
        // auto-respond in core; UI could log if needed
    }

    @Override
    public void onPong(Message msg) {
        // ignore or log heartbeat acknowledgments
    }

    @Override
    public void onError(Exception e) {
        System.err.println("[error] " + e.getMessage());
    }

    public static void main(String[] args) {
        Config cfg = new Config("/chat-client.properties");
        String host = cfg.get("client.host", "localhost");
        int port = cfg.getInt("client.port", 1234);
        Scanner initScanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = initScanner.nextLine().trim();

        ConsoleApp app = new ConsoleApp(host, port, username);
        try {
            app.start();
        } catch (IOException e) {
            System.err.println("Failed to start chat: " + e.getMessage());
        }
    }
}
