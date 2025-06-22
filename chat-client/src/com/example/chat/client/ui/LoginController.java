package com.example.chat.client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    TextField hostField, portField, userField;
    @FXML
    Button connectButton;
    public void initialize() {
        // load defaults from Config
    }
    @FXML
    private void onConnect() {
        // read host/port/user, hand off to MainApp to open ChatView
    }
}
