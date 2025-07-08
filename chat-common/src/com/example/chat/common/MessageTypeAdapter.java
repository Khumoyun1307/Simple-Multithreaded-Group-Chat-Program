package com.example.chat.common;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MessageTypeAdapter extends TypeAdapter<MessageType> {

    @Override
    public void write(JsonWriter out, MessageType value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.name());  // Serialize enum as string like "SECURE_TEXT"
        }
    }

    @Override
    public MessageType read(JsonReader in) throws IOException {
        String name = in.nextString();
        try {
            return MessageType.valueOf(name);  // Deserialize string back to enum
        } catch (IllegalArgumentException e) {
            return null;  // Or throw if you want strict validation
        }
    }
}