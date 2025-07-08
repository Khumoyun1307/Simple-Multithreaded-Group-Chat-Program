// src/com/example/chat/client/ui/ChatController.java
package com.example.chat.client.ui;

import com.example.chat.client.service.ChatService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

/**
 * Controller for ChatView.fxml. Binds the ListView to the service’s ObservableList,
 * and delegates send/stop actions to ChatService.
 */
public class ChatController {

    @FXML private TextField  searchField;
    @FXML private ListView<String> chatList;

    @FXML private ListView<String> messageList;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private ToggleButton themeToggle;

    private MainApp mainApp;
    private ChatService chatService;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void onToggleTheme() {
        themeToggle.setText(themeToggle.isSelected() ? "Light Mode" : "Dark Mode");
        mainApp.toggleTheme();
    }

    /**
     * Called by MainApp immediately after loading this controller.
     * Binds UI components to the ChatService.
     */
    public void initService(ChatService service) {
        this.chatService = service;
        // Bind the messages stream to the ListView
        messageList.setItems(service.getMessages());
    }

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (chatService == null || text.isEmpty()) return;

        if (text.startsWith("/secure ")) {
            String[] parts = text.split("\\s+", 3);
            if (parts.length < 3) {
                chatService.getMessages().add("Usage: /secure <user> <message>");
            } else {
                chatService.sendSecure(parts[1], parts[2]);
            }
        } else if (text.startsWith("/sall ")) {
            String msg = text.substring(6);
            chatService.sendSecureGroupMessage(msg);
        } else {
            chatService.sendText(text);
        }

        inputField.clear();
    }

    /**
     * If you need to do any cleanup when switching away from this view,
     * you can call chatService.stop() here (though MainApp.stop will also handle it).
     */
    public void stop() {
        if (chatService != null) {
            chatService.stop();
        }
    }
}
