package com.example.chat.client.auth.service;

import com.yourorg.auth.domain.model.User;
import com.yourorg.auth.domain.service.AuthManager;
import com.yourorg.auth.domain.exception.AuthException;
import com.cipherchat.engine.KeyManager;
import com.cipherchat.engine.CryptoException;

import java.security.KeyPair;

/**
 * Encapsulates the authentication use cases (login and signup) for the chat client.
 * Keeps business logic out of the UI controllers.
 */
public class AuthUseCase {
    private final AuthManager authManager;

    public AuthUseCase(AuthManager authManager) {
        this.authManager = authManager;
    }

    /**
     * Performs login and returns the auth token.
     * @throws AuthException on invalid credentials or other auth errors
     */
    public String login(String username, String password) throws AuthException {
        return authManager.login(username, password);
    }

    /**
     * Registers a new user, generates and saves a key pair, and returns the created User.
     * @throws AuthException on registration errors
     * @throws CryptoException on key generation or storage errors
     */
    public User signUp(String username, String password) throws AuthException, CryptoException {
        // 1. Register user via AuthManager
        User user = authManager.register(username, password);

        // 2. Generate and persist key pair for secure messaging
        KeyPair keyPair = KeyManager.generateRSAKeyPair();
        KeyManager.saveKeyPair(username, keyPair);

        return user;
    }
}
