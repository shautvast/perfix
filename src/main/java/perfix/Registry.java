package perfix;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

public class Registry {

    private static final Map<String, List<Method>> methods = new ConcurrentHashMap<>();
    private static final double NANO_2_MILLI = 1000000D;
    private static final String HEADER1 = "Invoked methods, by duration desc:";
    private static final String HEADER2 = "Method name;#Invocations;Total duration;Average Duration";
    private static final String FOOTER = "----------------------------------------";

    static void add(Method method) {
        methods.computeIfAbsent(method.getName(), key -> new ArrayList<>()).add(method);
    }

    public static void report(PrintStream out) {
        out.println(HEADER1);
        out.println(HEADER2);
        sortedMethodsByDuration().entrySet().stream()
                .map(entry -> createReportLine(entry.getValue()))
                .forEach(out::println);
        out.println(FOOTER);
    }

    private static String createReportLine(Report report) {
        return report.name + ";"
                + report.invocations + ";"
                + (long) (report.totalDuration / NANO_2_MILLI) + ";"
                + (long) (report.average() / NANO_2_MILLI);
    }

    private static SortedMap<Long, Report> sortedMethodsByDuration() {
        SortedMap<Long, Report> sortedByTotal = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        methods.forEach((name, measurements) -> {
            LongAdder totalDuration = new LongAdder();
            measurements.stream()
                    .filter(Objects::nonNull)
                    .forEach(m -> totalDuration.add(m.getDuration()));
            sortedByTotal.put(totalDuration.longValue(), new Report(name, measurements.size(), totalDuration.longValue()));
        });
        return sortedByTotal;
    }

    static class Report {
        final String name;
        final int invocations;
        final long totalDuration;

        Report(String name, int invocations, long totalDuration) {
            this.name = name;
            this.invocations = invocations;
            this.totalDuration = totalDuration;
        }

        double average() {
            return (double) totalDuration / invocations;
        }
    }
}
