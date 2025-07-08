package com.example.chat.client.service;

import com.example.chat.client.ChatClient;
import com.example.chat.common.GsonFactory;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.cipherchat.engine.CryptoException;
import com.cipherchat.engine.KeyManager;
import com.cipherchat.engine.DefaultCryptoEngine;
import com.cipherchat.engine.AESGCMEngine;
import com.cipherchat.engine.RSAEngine;
import com.cipherchat.engine.SignatureEngine;
import com.cipherchat.engine.model.EncryptResult;
import com.cipherchat.engine.model.DecryptResult;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service layer that wraps ChatClient and exposes an ObservableList of messages,
 * handles text and secure messaging (direct and group).
 */
public class ChatService {
    private final ChatClient client;
    private final String username;
    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-service-listener");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Gson gson = GsonFactory.getGson();

    public ChatService(String host, int port, String username) throws IOException {
        this.client = new ChatClient(host, port);
        this.username = username;
    }

    public void start() throws IOException {
        client.connect();
        client.send(new Message(username, "ALL", MessageType.JOIN, ""));
        running.set(true);
        listenerExecutor.execute(this::receiveLoop);
    }

    private void receiveLoop() {
        try {
            while (running.get()) {
                Message msg = client.receive();
                if (msg == null) continue;
                switch (msg.getType()) {
                    case PING:
                        client.send(new Message(username, "SERVER", MessageType.PONG, ""));
                        break;
                    case PONG:
                        break;
                    case SECURE_TEXT:
                        String decrypted = decryptIncomingSecureMessage(msg);
                        Platform.runLater(() -> messages.add("[🔒] " + decrypted));
                        break;
                    default:
                        String formatted = msg.getFrom() + ": " + msg.getBody();
                        Platform.runLater(() -> messages.add(formatted));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Platform.runLater(() -> messages.add("[error] " + e.getMessage()));
        }
    }

    public void sendText(String text) {
        if (text == null || text.isEmpty()) return;
        client.send(new Message(username, "ALL", MessageType.TEXT, text));
    }

    /**
     * Sends a secure message to a specific user.
     */
    public void sendSecure(String recipient, String plainText) {
        try {
            String enc = encryptForUser(plainText, recipient);
            client.send(new Message(username, recipient, MessageType.SECURE_TEXT, enc));
        } catch (Exception e) {
            Platform.runLater(() -> messages.add("[secure send failed] " + e.getMessage()));
        }
    }

    /**
     * Sends a secure group message to all known users (excluding self).
     */
    public void sendSecureGroupMessage(String text) {
        try {
            // 1. Load sender keys
            KeyPair senderKeys = KeyManager.loadKeyPair(username);
            // 2. Create AES key and encrypt once
            SecretKey aesKey = KeyManager.generateAESKey();
            byte[] aad = username.getBytes(StandardCharsets.UTF_8);
            byte[] ivAndCipher = new AESGCMEngine().encrypt(
                    text.getBytes(StandardCharsets.UTF_8), aad, aesKey
            );
            byte[] iv = Arrays.copyOfRange(ivAndCipher, 0, AESGCMEngine.IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(ivAndCipher, AESGCMEngine.IV_LENGTH, ivAndCipher.length);

            // 3. Discover recipients
            File keyDir = new File("data/keys");
            File[] userDirs = keyDir.listFiles(File::isDirectory);
            if (userDirs == null) return;

            for (File userDir : userDirs) {
                String rec = userDir.getName();
                if (rec.equals(username)) continue;
                try {
                    KeyPair recKeys = KeyManager.loadKeyPair(rec);
                    byte[] wrappedKey = new RSAEngine().wrapKey(aesKey.getEncoded(), recKeys.getPublic());
                    byte[] sigData = concatenate(iv, cipherText, wrappedKey, aad);
                    byte[] signature = new SignatureEngine().sign(sigData, senderKeys.getPrivate());
                    EncryptResult encrypted = new EncryptResult(iv, cipherText, wrappedKey, signature, username);
                    String json = gson.toJson(encrypted);
                    client.send(new Message(username, rec, MessageType.SECURE_TEXT, json));
                } catch (Exception ex) {
                    // continue with next
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> messages.add("[secure group send failed] " + e.getMessage()));
        }
    }

    private String encryptForUser(String plainText, String recipient) throws CryptoException {
        KeyPair senderKeys = KeyManager.loadKeyPair(username);
        KeyPair recipientKeys = KeyManager.loadKeyPair(recipient);
        EncryptResult enc = new DefaultCryptoEngine().encrypt(
                plainText.getBytes(StandardCharsets.UTF_8),
                username,
                recipientKeys.getPublic(),
                senderKeys.getPrivate()
        );
        return gson.toJson(enc);
    }

    private String decryptIncomingSecureMessage(Message msg) {
        try {
            EncryptResult enc = gson.fromJson(msg.getBody(), EncryptResult.class);
            KeyPair recKeys = KeyManager.loadKeyPair(username);
            KeyPair sendKeys = KeyManager.loadKeyPair(enc.getSenderUsername());
            DecryptResult res = new DefaultCryptoEngine().decrypt(
                    enc,
                    recKeys.getPrivate(),
                    sendKeys.getPublic()
            );
            String text = new String(res.getPlainText(), StandardCharsets.UTF_8);
            return (res.isSignatureVerified() ? "[✔] " : "[❌] ")
                    + enc.getSenderUsername() + ": " + text;
        } catch (Exception e) {
            return "[decryption failed] " + e.getMessage();
        }
    }

    private byte[] concatenate(byte[]... arrays) {
        int total = 0;
        for (byte[] arr : arrays) total += arr.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, out, pos, arr.length);
            pos += arr.length;
        }
        return out;
    }

    public void stop() {
        running.set(false);
        try {
            client.send(new Message(username, "ALL", MessageType.LEAVE, ""));
            client.disconnect();
        } catch (Exception ignored) {}
        listenerExecutor.shutdownNow();
    }

    public ObservableList<String> getMessages() {
        return messages;
    }
}
