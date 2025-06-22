package com.example.chat.client.ui;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.example.chat.client.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Controller for the ChatView.fxml. Manages message display and sending.
 */
public class ChatController {
    @FXML private ListView<String> messageList;
    @FXML private TextField inputField;
    @FXML private Button sendButton;

    private MainApp mainApp;
    private ChatClient client;
    private String username;
    private volatile boolean running = true;

    /**
     * Setter for MainApp reference.
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Initialize the client and username for this controller.
     */
    public void initClient(ChatClient client, String username) {
        this.client = client;
        this.username = username;
    }

    /**
     * Start the listener thread to receive messages from the server.
     */
    public void startListener() {
        Thread listener = new Thread(() -> {
            try {
                while (running) {
                    Message msg = client.receive();
                    if (msg == null) continue;
                    switch (msg.getType()) {
                        case PING:
                            // reply with PONG
                            client.send(new Message(username, "SERVER", MessageType.PONG, ""));
                            break;
                        case PONG:
                            // ignore
                            break;
                        default:
                            // display message
                            String formatted = msg.getFrom() + ": " + msg.getBody();
                            Platform.runLater(() -> messageList.getItems().add(formatted));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "client-ui-listener");
        listener.setDaemon(true);
        listener.start();
    }

    /**
     * Event handler for the Send button and Enter key.
     */
    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null) {
            return;
        }
        client.send(new Message(username, "ALL", MessageType.TEXT, text));
        inputField.clear();
    }

    /**
     * Clean up when the scene is closed or application stops.
     */
    public void stop() {
        running = false;
    }
}
