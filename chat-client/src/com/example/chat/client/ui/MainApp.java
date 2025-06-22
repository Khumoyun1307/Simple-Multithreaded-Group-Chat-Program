// chat-client/src/main/java/com/example/chat/client/ui/MainApp.java
package com.example.chat.client.ui;

import com.example.chat.common.Config;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.example.chat.client.ChatClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application launcher that manages scene switching between Login and Chat views.
 */
public class MainApp extends Application {
    private Stage primaryStage;
    private ChatClient client;
    private String username;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Chat Application");
        showLoginView();
    }

    /**
     * Loads and displays the Login view.
     */
    public void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            // Give controller a reference back to this MainApp
            LoginController controller = loader.getController();
            controller.setMainApp(this);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * After login, initialize ChatClient and display the Chat view.
     * @param host the server host
     * @param port the server port
     * @param username the chosen username
     */
    public void showChatView(String host, int port, String username) {
        this.username = username;
        try {
            // Initialize client connection
            client = new ChatClient(host, port);
            client.connect();
            client.send(new Message(username, "ALL", MessageType.JOIN, ""));

            // Load Chat view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChatView.fxml"));
            Parent root = loader.load();

            // Give controller access to MainApp and networking
            ChatController controller = loader.getController();
            controller.setMainApp(this);
            controller.initClient(client, username);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.sizeToScene();
            primaryStage.show();

            // Start listening for messages
            controller.startListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays an error alert dialog on the JavaFX Application thread.
     * @param message the error message to show
     */
    public void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.initOwner(primaryStage);
            alert.showAndWait();
        });
    }

    @Override
    public void stop() {
        // Clean up connection
        if (client != null) {
            try {
                client.send(new Message(username, "ALL", MessageType.LEAVE, ""));
                client.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
