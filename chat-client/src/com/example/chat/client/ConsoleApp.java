package com.example.chat.client;

import com.example.chat.common.Config;
import java.util.Scanner;

/**
 * Simple console-based UI for chat, using configuration and handling graceful shutdown.
 */
public class ConsoleApp {
    public static void main(String[] args) {
        // Load configuration from classpath
        Config cfg = new Config("/chat-client.properties");
        String host = cfg.get("client.host", "localhost");
        int port = cfg.getInt("client.port", 1234);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Connecting to " + host + ":" + port);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();

        ChatClient client = new ChatClient(host, port);
        ClientController controller = new ClientController(client, username);
        try {
            controller.start();
            System.out.println("Connected! Type messages or '/quit' to exit.");
            while (true) {
                String line = scanner.nextLine();
                if (line.equalsIgnoreCase("/quit")) {
                    break;
                }
                controller.sendChat(line);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            controller.shutdown();
            System.out.println("Disconnected.");
        }
    }
}