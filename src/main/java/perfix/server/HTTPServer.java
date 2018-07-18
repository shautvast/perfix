package perfix.server;

import fi.iki.elonen.NanoHTTPD;
import perfix.Registry;
import perfix.server.json.Serializer;

import java.io.IOException;
import java.util.ArrayList;

public class HTTPServer extends NanoHTTPD {


    public HTTPServer(int port) {
        super(port);
    }

    public void start() {

        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println(" --- Perfix http server running. Point your browser to http://localhost:" + getListeningPort() + "/");
        } catch (IOException ioe) {
            System.err.println(" --- Couldn't start Perfix http server:\n" + ioe);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        switch (uri) {
            case "/report":
                return perfixMetrics();
            case "/callstack":
                return perfixCallstack();
            case "/clear":
                return clear();
            default:
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "NOT FOUND");
        }
    }

    private Response perfixMetrics() {
        try {
            return addCors(newFixedLengthResponse(Response.Status.OK, "application/json", Serializer.toJSONString(new ArrayList<>(Registry.sortedMethodsByDuration().values()))));
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(e.toString());
        }
    }

    private Response addCors(Response response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        return response;
    }

    private Response perfixCallstack() {
        try {
            return addCors(newFixedLengthResponse(Response.Status.OK, "application/json", Serializer.toJSONString(Registry.getCallStack())));
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(e.toString());
        }
    }

    private Response clear() {
        Registry.clear();
        try {
            return addCors(newFixedLengthResponse(Response.Status.OK, "application/json", Serializer.toJSONString(Registry.getCallStack())));
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(e.toString());
        }
    }

}
