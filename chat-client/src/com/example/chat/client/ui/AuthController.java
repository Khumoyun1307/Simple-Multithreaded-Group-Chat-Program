package com.example.chat.client.ui;

import com.cipherchat.engine.CryptoException;
import com.cipherchat.engine.KeyManager;
import com.yourorg.auth.domain.model.User;
import com.yourorg.auth.domain.exception.UserAlreadyExistsException;
import com.yourorg.auth.domain.exception.InvalidCredentialsException;
import com.yourorg.auth.domain.exception.AuthException;
import com.yourorg.auth.domain.service.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

import java.net.URL;
import java.security.KeyPair;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
    @FXML private TextField loginIdField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TabPane modeTab;
    @FXML private TextField signupUsernameField;
    @FXML private PasswordField signupPasswordField;
    @FXML private PasswordField signupConfirmField;
    @FXML private Hyperlink forgotPasswordLink;

    private MainApp mainApp;
    private AuthManager authManager;

    public void setMainApp(MainApp app) {
        this.mainApp = app;
        this.authManager = app.getAuthManager();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Optionally style or configure controls here
    }

    @FXML
    private void onLogin() {
        String username = loginIdField.getText().trim();
        String pwd = loginPasswordField.getText();
        if (username.isEmpty() || pwd.isEmpty()) {
            mainApp.showAlert("Please enter both username and password.");
            return;
        }
        try {
            String token = authManager.login(username, pwd);
            mainApp.setAuthToken(token);
            mainApp.showChatView("localhost", 1234, username);
        } catch (AuthException e) {
            mainApp.showAlert(e.getMessage());
        }
    }

    @FXML
    private void onSignUp() {
        String username = signupUsernameField.getText().trim();
        String pwd = signupPasswordField.getText();
        String confirm = signupConfirmField.getText();

        if (username.isEmpty() || pwd.isEmpty()) {
            mainApp.showAlert("Username and password cannot be empty.");
            return;
        }
        if (!pwd.equals(confirm)) {
            mainApp.showAlert("Passwords do not match.");
            return;
        }

        try {
            // 1. Register user
            User user = authManager.register(username, pwd);

            // 2. Generate and save key pair
            KeyPair keyPair = KeyManager.generateRSAKeyPair();
            KeyManager.saveKeyPair(username, keyPair);

            // 3. Show success
            Alert info = new Alert(Alert.AlertType.INFORMATION,
                    "Sign Up Successful!\nYour User ID is: " + user.getId(),
                    ButtonType.OK);
            info.setHeaderText("Sign Up Successful");
            info.initOwner(mainApp.getPrimaryStage());
            info.showAndWait();

            // 4. Prefill login
            loginIdField.setText(user.getUsername());
            loginPasswordField.clear();

        } catch (AuthException e) {
            mainApp.showAlert(e.getMessage());
        } catch (CryptoException e) {
            mainApp.showAlert("Key generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onForgotPassword() {
        mainApp.showAlert("Password recovery not implemented yet.");
    }
}