package com.example.chat.server;

import com.example.chat.common.Config;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for the chat server. Loads configuration, starts heartbeats, and handles shutdown.
 */
public class ChatServer {
    private final int port;
    private final ExecutorService pool;
    private final ScheduledExecutorService scheduler;
    private ServerSocket serverSocket;

    public ChatServer(int port, int poolSize) {
        this.port = port;
        this.pool = Executors.newFixedThreadPool(poolSize);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        try {
            // Initialize server socket
            serverSocket = new ServerSocket(port);
            System.out.println("Chat server started on port " + port);

            // Load heartbeat interval from config
            Config cfg = new Config("/chat-server.properties");
            int interval = cfg.getInt("heartbeat.intervalSeconds", 30);

            // Schedule periodic PINGs to all clients
            scheduler.scheduleAtFixedRate(
                    () -> ChatManager.getInstance().pingAll(),
                    interval, interval, TimeUnit.SECONDS
            );

            // Register shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            // Main accept loop
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getRemoteSocketAddress());
                try {
                    ClientSession session = new ClientSession(clientSocket);
                    pool.execute(session);
                } catch (IOException e) {
                    System.err.println("Failed to initialize client session: " + e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            // Exception indicates socket closure or error
            shutdown();
        }
    }

    public void shutdown() {
        System.out.println("Shutting down server...");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            pool.shutdownNow();
            scheduler.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Config cfg = new Config("/chat-server.properties");
        int port = cfg.getInt("server.port", 1234);
        int poolSize = cfg.getInt(
                "server.poolSize", Runtime.getRuntime().availableProcessors() * 2
        );

        ChatServer server = new ChatServer(port, poolSize);
        server.start();
    }
}