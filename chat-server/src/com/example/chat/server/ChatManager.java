package com.example.chat.server;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton managing all active client sessions and routing messages.
 */
public class ChatManager {
    private static final ChatManager INSTANCE = new ChatManager();
    private final ConcurrentMap<String, ClientSession> sessions = new ConcurrentHashMap<>();

    private ChatManager() {}

    public static ChatManager getInstance() {
        return INSTANCE;
    }

    public void addSession(String username, ClientSession session) {
        sessions.put(username, session);
        broadcast(new Message(username, "ALL", MessageType.JOIN, username + " has joined"));
    }

    public void removeSession(String username) {
        sessions.remove(username);
        broadcast(new Message(username, "ALL", MessageType.LEAVE, username + " has left"));
    }

    public void broadcast(Message message) {
        sessions.values().forEach(session -> session.sendMessage(message));
    }

    /**
     * Sends a PING to all connected clients to check liveness.
     */
    public void pingAll() {
        sessions.forEach((username, session) ->
                session.sendMessage(new Message("SERVER", username, MessageType.PING, ""))
        );
    }

    public boolean sendTo(String username, Message message) {
        ClientSession session = sessions.get(username);
        if (session != null) {
            session.sendMessage(message);
            return true;
        }
        return false;
    }
}