package com.example.chat.client.ui;

import com.example.chat.common.Config;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * Controller for the LoginView.fxml. Handles user input for host, port, and username.
 */
public class LoginController {
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField userField;
    @FXML private Button connectButton;

    private MainApp mainApp;

    /**
     * Called by the FXML loader after fields are injected.
     */
    @FXML
    private void initialize() {
        // Load default values from config
        Config cfg = new Config("/chat-client.properties");
        hostField.setText(cfg.get("client.host", "localhost"));
        portField.setText(String.valueOf(cfg.getInt("client.port", 1234)));
    }

    /**
     * Setter for MainApp reference, called by MainApp when loading this controller.
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Event handler for the Connect button.
     */
    @FXML
    private void onConnect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            mainApp.showAlert("Invalid port number");
            return;
        }
        String username = userField.getText().trim();
        if (username.isEmpty()) {
            mainApp.showAlert("Username cannot be empty");
            return;
        }
        // Switch to chat view
        mainApp.showChatView(host, port, username);
    }
}