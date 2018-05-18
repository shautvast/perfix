package perfix.server;

import fi.iki.elonen.NanoHTTPD;
import perfix.Registry;
import perfix.server.json.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class HTTPServer extends NanoHTTPD {


    public HTTPServer(int port) {
        super(port);
    }

    public void start() {

        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("\nHttpServer running! Point your browser to http://localhost:2048/ \n");
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri.equals("/report")) {
            return perfixMetrics();
        } else {
            return serveStaticContent(uri);
        }
    }

    private Response serveStaticContent(String uri) {
        if (uri.equals("/")) {
            uri = "/index.html";
        }
        try {
            InputStream stream = getClass().getResourceAsStream(uri);
            if (stream == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "resource not found");
            }
            return newFixedLengthResponse(Response.Status.OK, determineContentType(uri), readFile(stream));
        } catch (IOException e) {
            return newFixedLengthResponse(e.toString());
        }
    }

    private Response perfixMetrics() {
        try {
            return newFixedLengthResponse(Response.Status.OK, "application/json", Serializer.toJSONString(new ArrayList<>(Registry.sortedMethodsByDuration().values())));
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(e.toString());
        }
    }

    private String readFile(InputStream stream) throws IOException {
        int nbytes = stream.available();
        byte[] bytes = new byte[nbytes];
        stream.read(bytes);
        return new String(bytes);
    }

    private String determineContentType(String uri) {
        if (uri.endsWith(".js")) {
            return "application/javascript";
        } else if (uri.endsWith(".css")) {
            return "text/css";
        } else {
            return "text/html";
        }
    }
}
