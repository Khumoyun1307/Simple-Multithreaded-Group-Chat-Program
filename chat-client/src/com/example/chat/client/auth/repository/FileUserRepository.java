package com.example.chat.client.auth.repository;

import com.yourorg.auth.domain.model.User;
import com.yourorg.auth.domain.repository.UserRepository;
import com.example.chat.common.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-backed implementation of UserRepository.
 * Loads users.json at startup and persists on save/update.
 */
public class FileUserRepository implements UserRepository {
    private static final Path FILE = Paths.get("users.json");
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Gson gson = GsonFactory.getGson();

    public FileUserRepository() {
        if (Files.exists(FILE)) {
            try {
                String json = Files.readString(FILE);
                Type listType = new TypeToken<List<User>>() {}.getType();
                List<User> list = gson.fromJson(json, listType);
                if (list != null) {
                    list.forEach(u -> users.put(u.getUsername(), u));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public synchronized void save(User user) {
        users.put(user.getUsername(), user);
        persist();
    }

    @Override
    public synchronized void update(User user) {
        users.put(user.getUsername(), user);
        persist();
    }

    private void persist() {
        try {
            List<User> list = new ArrayList<>(users.values());
            String json = gson.toJson(list);
            Files.writeString(FILE, json,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}