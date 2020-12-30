package perfix.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import perfix.server.json.Serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class HTTPServer implements HttpHandler {

    private static final String DEFAULT_ROUTE = "DEFAULT";
    private final int port;
    private final ConcurrentMap<String, Function<HttpExchange, ?>> routes = new ConcurrentHashMap<>();

    public HTTPServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/", this);
            server.setExecutor(Executors.newFixedThreadPool(3));
            server.start();

            PerfixController perfixController = new PerfixController();
            routes.put("/report", perfixController::perfixMetrics);
            routes.put("/callstack", perfixController::perfixCallstack);
            routes.put("/clear", perfixController::clear);
            routes.put(DEFAULT_ROUTE, this::staticContent);

            System.out.println(" --- Perfix http server running. Point your browser to http://localhost:" + port + "/");
        } catch (IOException ioe) {
            System.err.println(" --- Couldn't start Perfix http server:\n" + ioe);
        }
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        InputStream response = getResponse(exchange);

        OutputStream outputStream = exchange.getResponseBody();
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        int length = response.available();
        exchange.sendResponseHeaders(200, length);

        for (int i = 0; i < length; i++) {
            outputStream.write(response.read());
        }
        outputStream.flush();
        outputStream.close();
    }

    private InputStream getResponse(HttpExchange exchange) {
        String uri = exchange.getRequestURI().toString();
        Object response;
        if (routes.get(uri) != null) {
            response = routes.get(uri).apply(exchange);
        } else {
            response = routes.get(DEFAULT_ROUTE).apply(exchange);
        }
        if (response instanceof InputStream) {
            return (InputStream) response;
        } else {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            return toStream(Serializer.toJSONString(response));
        }
    }

    private InputStream staticContent(HttpExchange exchange) {
        String uri = exchange.getRequestURI().toString();
        if (uri.equals("/")) {
            uri = "/index.html";
        }
        InputStream resource = getClass().getResourceAsStream(uri);
        if (resource != null) {
            String mimeType;
            if (uri.endsWith("css")) {
                mimeType = "text/css";
            } else if (uri.endsWith("js")) {
                mimeType = "application/ecmascript";
            } else if (uri.equals("/favicon.ico")) {
                mimeType = "image/svg+xml";
            } else {
                mimeType = "text/html";
            }
            exchange.getResponseHeaders().add("Content-Type", mimeType);
            return resource;

        } else {
            return toStream(notFound());
        }
    }

    private String notFound() {
        return "NOT FOUND";
    }

    private InputStream toStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
