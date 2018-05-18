package perfix;

import perfix.server.HTTPServer;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

public class Agent {

    private static final String PORT_PROPERTY = "perfix.port";
    private static final String INCLUDES_PROPERTY = "perfix.includes";

    private static final String DEFAULT_PORT = "2048";
    private static final String MESSAGE = " --- Perfix agent active --- ";


    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println(MESSAGE);

        int port = Integer.parseInt(System.getProperty(PORT_PROPERTY, DEFAULT_PORT));

        new ClassInstrumentor(determineIncludes()).instrumentCode(inst);

        new HTTPServer(port).start();
    }

    private static List<String> determineIncludes() {
        String includesPropertyValue = System.getProperty(INCLUDES_PROPERTY);
        if (includesPropertyValue==null){
            System.out.println("WARNING: perfix.includes not set ");
            return Collections.emptyList();
        } 
        return new ArrayList<>(asList(includesPropertyValue.split(",")));
    }
}

