package za.ac.skiet.vyou2.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stands in for Resend's HTTP API in tests: captures each {@code POST /emails} body (parsed JSON)
 * instead of sending anything, so a test can inspect exactly what a real send would have contained.
 */
public final class FakeResendServer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final List<Map<String, Object>> received = new CopyOnWriteArrayList<>();

    public FakeResendServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        server.createContext("/emails", exchange -> {
            try {
                byte[] body = exchange.getRequestBody().readAllBytes();
                received.add(MAPPER.readValue(body, Map.class));
                byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    /** The "text" body of the most recently sent email. */
    public String lastEmailText() {
        if (received.isEmpty()) {
            throw new IllegalStateException("No email was sent");
        }
        return (String) received.get(received.size() - 1).get("text");
    }

    public int count() {
        return received.size();
    }

    public void clear() {
        received.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
