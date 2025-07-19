package com.example.chat.client.core;

import com.cipherchat.engine.KeyManager;
import com.cipherchat.engine.CryptoException;
import com.cipherchat.engine.CryptoEngine;
import com.cipherchat.engine.DefaultCryptoEngine;
import com.cipherchat.engine.model.DecryptResult;
import com.cipherchat.engine.model.EncryptResult;
import com.example.chat.common.GsonFactory;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import java.io.File;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Manages user interactions: sending JOIN, TEXT, LEAVE, PING/PONG, and secure messages.
 */
public class ClientController {
    private final ChatClient client;
    private final String username;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final CryptoEngine engine = new DefaultCryptoEngine();
    private final Gson gson = GsonFactory.getGson();

    public ClientController(ChatClient client, String username) {
        this.client = client;
        this.username = username;
    }

    public void start() throws Exception {
        client.connect();
        client.send(new Message(username, "ALL", MessageType.JOIN, ""));
        executor.execute(this::processIncoming);
    }

    public void shutdown() {
        running.set(false);
        client.send(new Message(username, "ALL", MessageType.LEAVE, ""));
        client.disconnect();
        executor.shutdown();
    }

    public void sendChat(String body) {
        client.send(new Message(username, "ALL", MessageType.TEXT, body));
    }

    public void sendSecureChat(String plainText, String recipientUsername) {
        try {
            String encryptedJson = encryptForUser(plainText, recipientUsername);
            client.send(new Message(username, recipientUsername, MessageType.SECURE_TEXT, encryptedJson));
        } catch (CryptoException e) {
            System.err.println("Encryption failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendSecureGroupMessage(String text) {
        try {
            KeyPair senderKeys = KeyManager.loadKeyPair(username);
            byte[] aad = username.getBytes(UTF_8);
            SecretKey aesKey = engine.generateAESKey();
            byte[] ivAndCipher = engine.aesGcmEncrypt(text.getBytes(UTF_8), aad, aesKey);

            File keyDir = new File("data/keys");
            File[] userDirs = keyDir.listFiles(File::isDirectory);
            if (userDirs == null) return;

            for (File userDir : userDirs) {
                String recipient = userDir.getName();
                if (recipient.equals(username)) continue;

                try {
                    PublicKey recipientPub = KeyManager.loadKeyPair(recipient).getPublic();
                    byte[] wrappedKey = engine.wrapKey(aesKey.getEncoded(), recipientPub);
                    byte[] sigInput = engine.prepareSignatureData(ivAndCipher, wrappedKey, aad);
                    byte[] signature = engine.signData(sigInput, senderKeys.getPrivate());
                    EncryptResult enc = engine.createEncryptedResult(ivAndCipher, wrappedKey, signature, username);
                    String jsonBody = gson.toJson(enc);
                    client.send(new Message(username, recipient, MessageType.SECURE_TEXT, jsonBody));
                } catch (Exception e) {
                    System.err.println("Failed to send secure message to " + recipient + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Secure group message failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processIncoming() {
        try {
            while (running.get()) {
                Message msg = client.receive();
                switch (msg.getType()) {
                    case PING:
                        client.send(new Message(username, "SERVER", MessageType.PONG, ""));
                        break;
                    case SECURE_TEXT:
                        System.out.println(decryptIncomingSecureMessage(msg));
                        break;
                    case PONG:
                    default:
                        System.out.println(msg.getFrom() + ": " + msg.getBody());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String encryptForUser(String plainText, String recipientUsername) throws CryptoException {
        KeyPair senderKeys = KeyManager.loadKeyPair(username);
        PublicKey recipientPub = KeyManager.loadKeyPair(recipientUsername).getPublic();
        EncryptResult encrypted = engine.encrypt(
                plainText.getBytes(UTF_8),
                username,
                recipientPub,
                senderKeys.getPrivate()
        );
        return gson.toJson(encrypted);
    }

    private String decryptIncomingSecureMessage(Message msg) {
        try {
            EncryptResult encrypted = gson.fromJson(msg.getBody(), EncryptResult.class);
            KeyPair recipientKeys = KeyManager.loadKeyPair(username);
            PublicKey senderPub = KeyManager.loadKeyPair(encrypted.getSenderUsername()).getPublic();
            DecryptResult result = engine.decrypt(encrypted, recipientKeys.getPrivate(), senderPub);
            String payload = new String(result.getPlainText(), UTF_8);
            return String.format("[secure] %s: %s %s",
                    result.getSenderUsername(), payload,
                    result.isSignatureVerified() ? "✅" : "❌");
        } catch (Exception e) {
            e.printStackTrace();
            return "[secure] Message decryption failed: " + e.getMessage();
        }
    }
}
