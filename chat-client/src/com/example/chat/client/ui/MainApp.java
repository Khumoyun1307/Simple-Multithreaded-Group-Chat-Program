package com.example.chat.client.ui;

import com.example.chat.common.Config;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.example.chat.client.ChatClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX application providing a modern GUI for the chat client.
 */
public class MainApp extends Application {
    private Stage primaryStage;
    private Scene loginScene;
    private Scene chatScene;

    // Login controls
    private TextField hostField;
    private TextField portField;
    private TextField userField;
    private Button connectButton;

    // Chat controls
    private ObservableList<String> messages;
    private ListView<String> messageListView;
    private TextField inputField;
    private Button sendButton;

    // Networking
    private ChatClient client;
    private String username;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Chat Application");

        initLoginScene();
        primaryStage.setScene(loginScene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void initLoginScene() {
        // Load defaults from config
        Config cfg = new Config("/chat-client.properties");
        String defaultHost = cfg.get("client.host", "localhost");
        int defaultPort = cfg.getInt("client.port", 1234);

        Label title = new Label("Welcome to Chat");
        title.setFont(Font.font(24));

        hostField = new TextField(defaultHost);
        hostField.setPromptText("Server Host");
        portField = new TextField(String.valueOf(defaultPort));
        portField.setPromptText("Port");
        userField = new TextField();
        userField.setPromptText("Username");

        connectButton = new Button("Connect");
        connectButton.setDefaultButton(true);
        connectButton.setOnAction(evt -> connect());

        VBox vbox = new VBox(10, title, hostField, portField, userField, connectButton);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        loginScene = new Scene(vbox);
    }

    private void initChatScene() {
        messages = FXCollections.observableArrayList();
        messageListView = new ListView<>(messages);
        messageListView.setPrefSize(400, 300);

        inputField = new TextField();
        inputField.setPromptText("Type your message...");
        inputField.setPrefWidth(300);
        inputField.setOnAction(evt -> sendMessage());

        sendButton = new Button("Send");
        sendButton.setOnAction(evt -> sendMessage());

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        inputBox.setAlignment(Pos.CENTER);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(messageListView);
        borderPane.setBottom(inputBox);

        chatScene = new Scene(borderPane, 420, 360);
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Invalid port number.");
            return;
        }
        username = userField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Username cannot be empty.");
            return;
        }

        try {
            client = new ChatClient(host, port);
            client.connect();
            // send JOIN
            client.send(new Message(username, "ALL", MessageType.JOIN, ""));

            initChatScene();
            primaryStage.setScene(chatScene);

            // start listener thread
            new Thread(this::receiveMessages, "ui-receiver").start();
        } catch (IOException e) {
            showAlert("Failed to connect to server: " + e.getMessage());
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                Message msg = client.receive();
                if (msg == null) continue;
                switch (msg.getType()) {
                    case PING:
                        // auto-respond
                        client.send(new Message(username, "SERVER", MessageType.PONG, ""));
                        break;
                    case PONG:
                        // ignore
                        break;
                    default:
                        String text = String.format("%s: %s", msg.getFrom(), msg.getBody());
                        Platform.runLater(() -> messages.add(text));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        client.send(new Message(username, "ALL", MessageType.TEXT, text));
        inputField.clear();
    }

    private void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            alert.showAndWait();
        });
    }

    @Override
    public void stop() {
        // send LEAVE and disconnect
        if (client != null) {
            client.send(new Message(username, "ALL", MessageType.LEAVE, ""));
            client.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
