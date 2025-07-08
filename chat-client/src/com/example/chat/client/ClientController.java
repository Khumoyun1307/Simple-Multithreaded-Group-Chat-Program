package com.example.chat.client;

import com.cipherchat.engine.model.DecryptResult;
import com.example.chat.common.*;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cipherchat.engine.*;
import com.cipherchat.engine.model.EncryptResult;
import com.google.gson.Gson;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Manages user interactions: sending JOIN, TEXT, LEAVE, responding to PING, and handling incoming messages.
 */
public class ClientController {
    private final ChatClient client;
    private final String username;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ClientController(ChatClient client, String username) {
        this.client = client;
        this.username = username;
    }

    public void start() throws Exception {
        client.connect();
        // Send JOIN message
        client.send(new Message(username, "ALL", MessageType.JOIN, ""));

        // Start a thread to handle incoming messages
        executor.execute(() -> {
            try {
                while (running.get()) {
                    Message msg = client.receive();
                    switch (msg.getType()) {
                        case PING:
                            client.send(new Message(username, "SERVER", MessageType.PONG, ""));
                            break;
                        case PONG:
                            break;
                        case SECURE_TEXT:
                            System.out.println(decryptIncomingSecureMessage(msg));
                            break;
                        default:
                            System.out.println(msg.getFrom() + ": " + msg.getBody());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
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
            Message secureMessage = new Message(
                    username,
                    recipientUsername,
                    MessageType.SECURE_TEXT,
                    encryptedJson
            );
            client.send(secureMessage);
        } catch (CryptoException e) {
            System.err.println("Encryption failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendSecureGroupMessage(String text) {
        try {
            // 1. Load sender keys
            KeyPair senderKeys = KeyManager.loadKeyPair(username);

            // 2. Create AES key and encrypt once
            CryptoEngine engine = new DefaultCryptoEngine();
            SecretKey aesKey = KeyManager.generateAESKey();
            byte[] aad = username.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] ivAndCipher = new AESGCMEngine().encrypt(
                    text.getBytes(java.nio.charset.StandardCharsets.UTF_8), aad, aesKey
            );
            byte[] iv = Arrays.copyOfRange(ivAndCipher, 0, AESGCMEngine.IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(ivAndCipher, AESGCMEngine.IV_LENGTH, ivAndCipher.length);

            // 3. Discover known users with keys (excluding self)
            File keyDir = new File("data/keys");
            File[] userDirs = keyDir.listFiles(File::isDirectory);
            if (userDirs == null) return;

            Gson gson = GsonFactory.getGson();

            for (File userDir : userDirs) {
                String recipient = userDir.getName();
                if (recipient.equals(username)) continue; // skip self

                try {
                    KeyPair recipientKeys = KeyManager.loadKeyPair(recipient);

                    // Wrap key for recipient
                    byte[] wrappedKey = new RSAEngine().wrapKey(aesKey.getEncoded(), recipientKeys.getPublic());

                    // Sign the full data
                    byte[] sigData = concatenate(iv, cipherText, wrappedKey, aad);
                    byte[] signature = new SignatureEngine().sign(sigData, senderKeys.getPrivate());

                    EncryptResult encrypted = new EncryptResult(iv, cipherText, wrappedKey, signature, username);
                    String jsonBody = gson.toJson(encrypted);

                    Message secureMessage = new Message(username, recipient, MessageType.SECURE_TEXT, jsonBody);
                    client.send(secureMessage);

                } catch (Exception e) {
                    System.err.println("Could not send secure message to " + recipient + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Secure group message failed: " + e.getMessage());
            e.printStackTrace();
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


    private String encryptForUser(String plainText, String recipientUsername) throws CryptoException {
        KeyPair senderKeys = KeyManager.loadKeyPair(username);
        KeyPair recipientKeys = KeyManager.loadKeyPair(recipientUsername);
        PublicKey recipientPublicKey = recipientKeys.getPublic();

        CryptoEngine engine = new DefaultCryptoEngine();
        EncryptResult encrypted = engine.encrypt(
                plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                username,
                recipientPublicKey,
                senderKeys.getPrivate()
        );

        Gson gson = GsonFactory.getGson();
        return gson.toJson(encrypted);
    }

    private String decryptIncomingSecureMessage(Message msg) {
        try {
            Gson gson = GsonFactory.getGson();
            EncryptResult encrypted = gson.fromJson(msg.getBody(), EncryptResult.class);

            KeyPair recipientKeys = KeyManager.loadKeyPair(username); // self
            KeyPair senderKeys = KeyManager.loadKeyPair(encrypted.getSenderUsername()); // sender
            CryptoEngine engine = new DefaultCryptoEngine();

            DecryptResult result = engine.decrypt(
                    encrypted,
                    recipientKeys.getPrivate(),
                    senderKeys.getPublic()
            );

            return "[secure] " + result.getSenderUsername() + ": " +
                    new String(result.getPlainText(), java.nio.charset.StandardCharsets.UTF_8) +
                    (result.isSignatureVerified() ? " ✅" : " ❌");

        } catch (Exception e) {
            e.printStackTrace();
            return "[secure] Message decryption failed: " + e.getMessage();
        }
    }
}