import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer {
    private final ObjectMapper mapper;
    private final TaskStore store;
    private final int port;
    private HttpServer server;

    public WebServer(ObjectMapper mapper, TaskStore store, int port) {
        this.mapper = mapper;
        this.store = store;
        this.port = port;
    }

    public void start(boolean openBrowser) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/tasks", this::handleTasks);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        String url = "http://127.0.0.1:" + port;
        System.out.println("TaskTracker is running at " + url);
        System.out.println("Data file: " + store.getDataFile());
        System.out.println("Press Ctrl+C to stop.");

        if (openBrowser && !openBrowser(url)) {
            System.out.println("Open " + url + " in your browser.");
        }
    }

    private boolean openBrowser(String url) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder builder;
            if (os.contains("win")) {
                builder = new ProcessBuilder("cmd", "/c", "start", "", url);
            } else if (os.contains("mac")) {
                builder = new ProcessBuilder("open", url);
            } else {
                builder = new ProcessBuilder("xdg-open", url);
            }
            builder.start();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        addCors(exchange.getResponseHeaders());
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String suffix = path.substring("/api/tasks".length());

            if (suffix.isEmpty() || "/".equals(suffix)) {
                if ("GET".equalsIgnoreCase(method)) {
                    sendJson(exchange, 200, store.load());
                    return;
                }
                if ("POST".equalsIgnoreCase(method)) {
                    Map<?, ?> body = readJsonMap(exchange);
                    String description = stringValue(body.get("description"));
                    if (description == null || description.isBlank()) {
                        sendError(exchange, 400, "description is required");
                        return;
                    }
                    Task created = store.add(description.trim());
                    String status = stringValue(body.get("status"));
                    if (status != null && !"todo".equals(status)) {
                        created = store.update(created.id, null, normalizeStatus(status));
                    }
                    sendJson(exchange, 201, created);
                    return;
                }
            } else {
                int id;
                try {
                    id = Integer.parseInt(suffix.replace("/", ""));
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "invalid task id");
                    return;
                }

                if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                    Map<?, ?> body = readJsonMap(exchange);
                    String description = body.containsKey("description") ? stringValue(body.get("description")) : null;
                    String status = body.containsKey("status") ? normalizeStatus(stringValue(body.get("status"))) : null;
                    if (description != null && description.isBlank()) {
                        sendError(exchange, 400, "description cannot be empty");
                        return;
                    }
                    Task updated = store.update(id, description == null ? null : description.trim(), status);
                    if (updated == null) {
                        sendError(exchange, 404, "task not found");
                        return;
                    }
                    sendJson(exchange, 200, updated);
                    return;
                }

                if ("DELETE".equalsIgnoreCase(method)) {
                    if (!store.delete(id)) {
                        sendError(exchange, 404, "task not found");
                        return;
                    }
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
            }

            sendError(exchange, 405, "method not allowed");
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "internal server error");
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..")) {
            sendText(exchange, 400, "Bad request", "text/plain; charset=utf-8");
            return;
        }

        String resource = "/web" + path;
        try (InputStream in = WebServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                sendText(exchange, 404, "Not found", "text/plain; charset=utf-8");
                return;
            }
            byte[] data = in.readAllBytes();
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType(path));
            headers.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        }
    }

    private Map<?, ?> readJsonMap(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) return new HashMap<>();
        return mapper.readValue(bytes, HashMap.class);
    }

    private String normalizeStatus(String value) {
        if (value == null) return null;
        return switch (value) {
            case "todo", "in-progress", "done" -> value;
            default -> throw new IllegalArgumentException("invalid status");
        };
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] data = mapper.writeValueAsBytes(body);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, String> error = Map.of("error", message == null ? "error" : message);
        sendJson(exchange, status, error);
    }

    private void sendText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }

    private void addCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
