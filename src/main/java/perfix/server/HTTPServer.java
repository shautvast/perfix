package perfix.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import perfix.Registry;
import perfix.server.json.Serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class HTTPServer implements HttpHandler {
    private static final Logger log = Logger.getLogger("perfix");
    private final int port;

    public HTTPServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/", this);
            server.setExecutor(Executors.newFixedThreadPool(3));
            server.start();
            System.out.println(" --- Perfix http server running. Point your browser to http://localhost:" + port + "/");
        } catch (IOException ioe) {
            System.err.println(" --- Couldn't start Perfix http server:\n" + ioe);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().toString();
        InputStream response = null;
        switch (uri) {
            case "/report":
                setContentTypeJson(exchange);
                response = toStream(perfixMetrics());
                break;
            case "/callstack":
                setContentTypeJson(exchange);
                response = toStream(perfixCallstack());
                break;
            case "/clear":
                setContentTypeJson(exchange);
                response = toStream(clear());
                break;
            default:
                response = staticContent(exchange, uri);
        }
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

    private void setContentTypeJson(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    private InputStream staticContent(HttpExchange exchange, String uri) {
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


    private String perfixMetrics() {
        try {
            return Serializer.toJSONString(new ArrayList<>(Registry.sortedMethodsByDuration().values()));
        } catch (Exception e) {
            log.severe(e.toString());
            return e.toString();
        }
    }


    private InputStream toStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private String perfixCallstack() {
        try {
            return Serializer.toJSONString(Registry.getCallStack());
        } catch (Exception e) {
            log.severe(e.toString());
            return e.toString();
        }
    }

    private String clear() {
        Registry.clear();
        try {
            return Serializer.toJSONString(Registry.getCallStack());
        } catch (Exception e) {
            log.severe(e.toString());
            return e.toString();
        }
    }


}
