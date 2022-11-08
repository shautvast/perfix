package perfix;

import sqlighter.data.Value;
import perfix.instrument.Instrumentor;
import sqlighter.DatabaseBuilder;
import sqlighter.data.Record;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import static java.util.Arrays.asList;

public class Agent {

    private static final String INCLUDES_PROPERTY = "perfix.includes";

    private static final String MESSAGE = " --- Perfix agent active";

    private static final DatabaseBuilder databaseBuilder = new DatabaseBuilder();

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println(MESSAGE);
        Instrumentor.create(determineIncludes()).instrumentCode(instrumentation);
        System.out.println("Instrumenting " + System.getProperty(INCLUDES_PROPERTY));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LongAdder rowid = new LongAdder();
            try {
                Registry.sortedMethodsByDuration().values()
                        .forEach(r -> {
                            rowid.increment();
                            Record record = new Record(rowid.intValue());
                            record.addValues(Value.of(r.getName()), Value.of(r.getInvocations()), Value.of(r.getAverage() / 1_000_000F), Value.of(r.getTotalDuration() / 1_000_000F));
                            databaseBuilder.addRecord(record);
                        });
                databaseBuilder.addSchema("results", "create table results(name varchar(100), invocations integer, average float, total float)");
                databaseBuilder.build().write(Files.newByteChannel(Paths.get("results.sqlite"), StandardOpenOption.WRITE, StandardOpenOption.CREATE));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private static List<String> determineIncludes() {
        String includesPropertyValue = System.getProperty(INCLUDES_PROPERTY).replaceAll("\\.", "/");
        if (includesPropertyValue == null) {
            System.out.println("WARNING: perfix.includes not set ");
            return Collections.emptyList();
        } else {
            ArrayList<String> includes = new ArrayList<>(asList(includesPropertyValue.split(",")));
            System.out.println("includes classes: " + includes + "*");
            return includes;
        }
    }
}

