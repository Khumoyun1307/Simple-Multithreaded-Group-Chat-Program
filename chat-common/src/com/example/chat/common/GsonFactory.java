package com.example.chat.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;

public class GsonFactory {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(MessageType.class, new MessageTypeAdapter())
            .create();

    public static Gson getGson() {
        return GSON;
    }
}