package com.example.chat.client.ui.chat.controllers;

import com.example.chat.client.ui.MainApp;
import com.example.chat.client.ui.chat.adapters.ChatServiceFX;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import java.io.IOException;

/**
 * Controller for ChatView.fxml. Uses ChatServiceFX for all chat logic.
 */
public class ChatController {

    @FXML private TextField searchField;
    @FXML private ListView<String> messageList;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private ToggleButton themeToggle;

    private MainApp mainApp;
    private ChatServiceFX chatService;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Initialize ChatServiceFX and bind messages to ListView.
     */
    public void initService(String host, int port, String username) {
        chatService = new ChatServiceFX(host, port, username);
        messageList.setItems(chatService.getMessages());
        try {
            chatService.connect();
        } catch (IOException e) {
            mainApp.showAlert("Failed to connect: " + e.getMessage());
        }
    }

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("/secure ")) {
            String[] parts = text.split("\\s+", 3);
            if (parts.length < 3) {
                messageList.getItems().add("Usage: /secure <user> <message>");
            } else {
                chatService.sendSecure(parts[1], parts[2]);
            }
        } else if (text.startsWith("/sall ")) {
            chatService.sendSecureGroup(text.substring(6).trim());
        } else if (text.startsWith("/join ")) {
            chatService.joinRoom(text.substring(6).trim());
        } else if (text.startsWith("/leave ")) {
            chatService.leaveRoom(text.substring(7).trim());
        } else {
            chatService.sendText("ALL", text);
        }
        inputField.clear();
    }

    @FXML
    private void onToggleTheme() {
        themeToggle.setText(themeToggle.isSelected() ? "Light Mode" : "Dark Mode");
        mainApp.toggleTheme();
    }

    public void stop() {
        if (chatService != null) {
            chatService.disconnect();
        }
    }
}
