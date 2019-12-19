package perfix;

import perfix.instrument.Instrumentor;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class Agent {

    public static final String DBFILE_PROPERTY = "perfix.db";
    public static final String DEFAULT_DBFILE = "perfix.db";
    private static final String PORT_PROPERTY = "perfix.port";
    private static final String INCLUDES_PROPERTY = "perfix.includes";
    private static final String MESSAGE = " --- Perfix agent active";

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println(MESSAGE);
        String dbFile = System.getProperty(DBFILE_PROPERTY);
        if (dbFile == null) {
            dbFile = DEFAULT_DBFILE;
        }
        System.out.println(" --- SQLite file written to " + dbFile);

        Instrumentor.create(determineIncludes()).instrumentCode(instrumentation);
    }

    private static List<String> determineIncludes() {
        String includesPropertyValue = System.getProperty(INCLUDES_PROPERTY);
        if (includesPropertyValue == null) {
            System.out.println("WARNING: perfix.includes not set ");
            return Collections.emptyList();
        }
        System.out.println(" --- Instrumenting packages: " + includesPropertyValue + ".*");
        return new ArrayList<>(asList(includesPropertyValue.split(",")));
    }
}

