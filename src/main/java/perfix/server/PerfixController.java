package perfix.server;

import com.sun.net.httpserver.HttpExchange;
import perfix.MethodNode;
import perfix.Registry;
import perfix.Report;

import java.util.ArrayList;
import java.util.List;

public class PerfixController {

    public  List<Report> perfixMetrics(HttpExchange exchange) {
        return new ArrayList<>(Registry.sortedMethodsByDuration().values());
    }

    public  List<MethodNode> perfixCallstack(HttpExchange exchange) {
        return Registry.getCallStack();
    }

    public  String clear(HttpExchange exchange) {
        Registry.clear();
        return "clear";
    }
}
