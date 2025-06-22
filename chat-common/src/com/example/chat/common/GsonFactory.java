package com.example.chat.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;

/**
 * Provides a preconfigured Gson instance for client & server.
 */
public class GsonFactory {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .create();

    public static Gson getGson() {
        return GSON;
    }
}
