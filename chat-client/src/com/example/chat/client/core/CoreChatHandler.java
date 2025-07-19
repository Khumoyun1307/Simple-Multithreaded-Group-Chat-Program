package com.example.chat.client.core;

import com.cipherchat.engine.*;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.example.chat.common.GsonFactory;
import com.cipherchat.engine.model.EncryptResult;
import com.cipherchat.engine.model.DecryptResult;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CoreChatHandler: handles network I/O, message routing, encryption/decryption,
 * and emits events to registered listeners. No UI code here.
 */
public class CoreChatHandler {
    private final ChatClient client;
    private final String username;
    private final Gson gson = GsonFactory.getGson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<ChatEventListener> listeners = new CopyOnWriteArrayList<>();

    public CoreChatHandler(String host, int port, String username) {
        this.client = new ChatClient(host, port);
        this.username = username;
    }

    public void addListener(ChatEventListener l) {
        listeners.add(l);
    }

    public void removeListener(ChatEventListener l) {
        listeners.remove(l);
    }

    public void connect() throws IOException {
        client.connect();
        client.send(new Message(username, "ALL", MessageType.JOIN, ""));
        running.set(true);
        executor.execute(this::receiveLoop);
    }

    public void disconnect() {
        running.set(false);
        client.send(new Message(username, "ALL", MessageType.LEAVE, ""));
        client.disconnect();
        executor.shutdownNow();
    }

    public void sendText(String to, String body) {
        client.send(new Message(username, to, MessageType.TEXT, body));
    }

    public void sendSecure(String to, String body) throws CryptoException {
        KeyPair sender = KeyManager.loadKeyPair(username);
        KeyPair rec    = KeyManager.loadKeyPair(to);
        EncryptResult result = new DefaultCryptoEngine().encrypt(
                body.getBytes(StandardCharsets.UTF_8),
                username,
                rec.getPublic(),
                sender.getPrivate()
        );
        client.send(new Message(username, to, MessageType.SECURE_TEXT, gson.toJson(result)));
    }

    /**
     * Sends a secure group message using AES-GCM once, then RSA-wrap and sign per user.
     */
    public void sendSecureGroup(String text) {
        try {
            KeyPair senderKeys = KeyManager.loadKeyPair(username);
            // 1. Generate AES key and encrypt message once
            SecretKey aesKey = KeyManager.generateAESKey();
            byte[] aad = username.getBytes(StandardCharsets.UTF_8);
            byte[] ivAndCipher = new AESGCMEngine().encrypt(
                    text.getBytes(StandardCharsets.UTF_8), aad, aesKey
            );
            int ivLen = AESGCMEngine.IV_LENGTH;
            byte[] iv = Arrays.copyOfRange(ivAndCipher, 0, ivLen);
            byte[] cipherText = Arrays.copyOfRange(ivAndCipher, ivLen, ivAndCipher.length);

            // 2. Send to each user except self
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
                    EncryptResult enc = new EncryptResult(iv, cipherText, wrappedKey, signature, username);
                    client.send(new Message(username, rec, MessageType.SECURE_TEXT, gson.toJson(enc)));
                } catch (Exception e) {
                    listeners.forEach(l -> l.onError(e));
                }
            }
        } catch (Exception e) {
            listeners.forEach(l -> l.onError(e));
        }
    }

    public void joinRoom(String room) {
        client.send(new Message(username, room, MessageType.JOIN, ""));
    }

    public void leaveRoom(String room) {
        client.send(new Message(username, room, MessageType.LEAVE, ""));
    }

    public void sendRoomMessage(String room, String body) {
        client.send(new Message(username, room, MessageType.TEXT, body));
    }

    public List<String> listRooms() {
        throw new UnsupportedOperationException("listRooms() requires server support");
    }

    private void receiveLoop() {
        try {
            while (running.get()) {
                Message msg = client.receive();
                handleMessage(msg);
            }
        } catch (Exception e) {
            listeners.forEach(l -> l.onError(e));
        }
    }

    private void handleMessage(Message msg) {
        try {
            switch (msg.getType()) {
                case TEXT:
                    listeners.forEach(l -> l.onMessage(msg));
                    break;
                case SECURE_TEXT:
                    String plain = decryptIncoming(msg);
                    listeners.forEach(l -> l.onSecureMessage(msg, plain));
                    break;
                case JOIN:
                    listeners.forEach(l -> l.onUserJoined(msg));
                    break;
                case LEAVE:
                    listeners.forEach(l -> l.onUserLeft(msg));
                    break;
                case PING:
                    listeners.forEach(l -> l.onPing(msg));
                    client.send(new Message(username, msg.getFrom(), MessageType.PONG, ""));
                    break;
                case PONG:
                    listeners.forEach(l -> l.onPong(msg));
                    break;
                default:
                    // ignore
            }
        } catch (CryptoException e) {
            listeners.forEach(l -> l.onError(e));
        }
    }

    private String decryptIncoming(Message msg) throws CryptoException {
        EncryptResult enc = gson.fromJson(msg.getBody(), EncryptResult.class);
        KeyPair recKeys = KeyManager.loadKeyPair(username);
        KeyPair sndKeys = KeyManager.loadKeyPair(enc.getSenderUsername());
        DecryptResult dec = new DefaultCryptoEngine().decrypt(enc, recKeys.getPrivate(), sndKeys.getPublic());
        return new String(dec.getPlainText(), StandardCharsets.UTF_8);
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
}
