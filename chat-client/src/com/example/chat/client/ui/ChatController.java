package com.example.chat.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ChatController {
    @FXML
    ListView<String> messageList;
    @FXML
    TextField inputField;
    @FXML
    Button sendButton;
    public void initialize() {
        // optional: set up cell factories, placeholder text
    }
    @FXML
    private void onSend() {
        // grab inputField text, send over ChatClient, clear field
    }
    public void addMessage(String msg) {
        Platform.runLater(() -> messageList.getItems().add(msg));
    }
}
