package com.example.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple configuration loader for .properties files on the classpath.
 */
public class Config {
    private final Properties props = new Properties();

    /**
     * Loads the given resource (e.g. "/chat-server.properties") from the classpath.
     * @param resourcePath the classpath resource path, starting with '/'
     */
    public Config(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from " + resourcePath, e);
        }
    }

    /**
     * Returns the property value for key, or defaultValue if absent.
     */
    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Returns the property as an int, or defaultValue if absent or malformed.
     */
    public int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Returns the property as a boolean, or defaultValue if absent.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            return Boolean.parseBoolean(val.trim());
        }
        return defaultValue;
    }
}
