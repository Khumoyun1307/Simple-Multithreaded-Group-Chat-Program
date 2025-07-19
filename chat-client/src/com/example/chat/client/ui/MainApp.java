package com.example.chat.client.ui;

import com.example.chat.client.auth.repository.FileUserRepository;
import com.example.chat.client.ui.chat.adapters.ChatServiceFX;
import com.example.chat.client.ui.chat.controllers.ChatController;
import com.yourorg.auth.domain.security.Pbkdf2PasswordEncoder;
import com.yourorg.auth.domain.security.UuidTokenService;
import com.yourorg.auth.domain.service.AuthManager;
import com.yourorg.auth.domain.service.AuthManagerImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    private Stage primaryStage;
    private AuthManager authManager;
    private String authToken;

    private static final String LIGHT = "/css/light.css";
    private static final String DARK  = "/css/dark.css";

    private Scene chatScene;
    private boolean darkMode = false;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Initialize AuthManager (unchanged)
        authManager = new AuthManagerImpl(
                new FileUserRepository(),
                new Pbkdf2PasswordEncoder(),
                new UuidTokenService()
        );
        primaryStage.setTitle("Chat Application");
        showAuthView();
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void showAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuthView.fxml"));
            Parent root = loader.load();
            AuthController authController = loader.getController();
            authController.setMainApp(this);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/auth.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            showAlert("Failed to load Auth view:\n" + e.getMessage());
        }
    }

    public void showChatView(String host, int port, String username) {
        try {
            // 1) Initialize JavaFX adapter for core chat logic
            ChatServiceFX chatService = new ChatServiceFX(host, port, username);

            // 2) Load the Chat.fxml and wire the controller
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChatView.fxml"));
            Parent root = loader.load();
            ChatController controller = loader.getController();
            controller.setMainApp(this);
            controller.initService(host, port, username);

            // 3) Apply stylesheets (base + default theme)
            chatScene = new Scene(root);
            chatScene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
            chatScene.getStylesheets().add(getClass().getResource(LIGHT).toExternalForm());

            primaryStage.setScene(chatScene);
            primaryStage.setResizable(true);
            primaryStage.show();
        } catch (IOException e) {
            showAlert("Failed to start chat service:\n" + e.getMessage());
        }
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        chatScene.getStylesheets().removeAll(
                getClass().getResource(LIGHT).toExternalForm(),
                getClass().getResource(DARK).toExternalForm()
        );
        chatScene.getStylesheets().add(
                getClass().getResource(darkMode ? DARK : LIGHT).toExternalForm()
        );
    }

    public void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        if (primaryStage.getScene() != null) {
            alert.initOwner(primaryStage);
        }
        alert.showAndWait();
    }

    @Override
    public void stop() {
        // Nothing to stop here, UI adapter handles disconnect in controller
    }

    public static void main(String[] args) {
        launch(args);
    }
}
