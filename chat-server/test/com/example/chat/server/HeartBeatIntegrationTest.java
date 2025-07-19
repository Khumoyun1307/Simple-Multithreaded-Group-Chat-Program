// chat-server/src/test/java/com/example/chat/server/HeartbeatIntegrationTest.java
package com.example.chat.server;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import com.example.chat.client.core.ChatClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test to verify that the server sends PING heartbeats
 * to connected clients at the configured interval.
 *
 * Requires JUnit 5 on the classpath (junit-jupiter-api + engine).
 */
public class HeartBeatIntegrationTest {
    private static final int TEST_PORT = 54321;
    private static Thread serverThread;
    private static ChatServer server;

    @BeforeAll
    public static void setUp() {
        // Start the server with a small pool size
        server = new ChatServer(TEST_PORT, 1);
        serverThread = new Thread(server::start, "test-server");
        serverThread.start();
        // Wait briefly for server to bind
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    @AfterAll
    public static void tearDown() {
        server.shutdown();
        try { serverThread.join(1000); } catch (InterruptedException ignored) {}
    }

    @Test
    public void testHeartbeatPingReceived() throws Exception {
        // Create a client and connect
        ChatClient client = new ChatClient("localhost", TEST_PORT);
        client.connect();

        // Send JOIN to register
        client.send(new Message("tester", "ALL", MessageType.JOIN, ""));

        // First inbound message should be the JOIN broadcast
        Message first = client.receive();
        assertEquals(MessageType.JOIN, first.getType(), "Expected first message to be JOIN");

        // Next inbound message should be a PING within a second or two
        Message ping = client.receive();
        assertEquals(MessageType.PING, ping.getType(), "Expected a PING heartbeat");
    }
}
