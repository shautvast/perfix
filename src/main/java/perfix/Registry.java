package perfix;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

public class Registry {

    static final Map<String, List<Method>> methods = new ConcurrentHashMap<>();

    static void add(Method method) {
        methods.computeIfAbsent(method.getName(), key -> new ArrayList<>()).add(method);
    }

    public static void report() {
        System.out.println("Invoked methods, by duration desc:");
        sortedMethodsByDuration().forEach((k, report) -> {
            System.out.println("method: " + report.name);
            System.out.println("\tInvocations: " + report.invocations);
            System.out.println("\tTotal duration: " + report.totalDuration + " nanosecs.");
            System.out.println("\tAverage duration " + report.average() + " nanosecs.");
        });
    }

    private static SortedMap<Long, Report> sortedMethodsByDuration() {
        SortedMap<Long, Report> sortedByTotal = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        methods.forEach((name, results) -> {
            LongAdder adder = new LongAdder();
            long totalDuration = results.stream().peek((r) -> adder.increment()).mapToLong(r -> r.getDuration()).sum();
            sortedByTotal.put(totalDuration, new Report(name, adder.longValue(), totalDuration));
        });
        return sortedByTotal;
    }

    static class Report {
        final String name;
        final long invocations;
        final long totalDuration;

        Report(String name, long invocations, long totalDuration) {
            this.name = name;
            this.invocations = invocations;
            this.totalDuration = totalDuration;
        }

        double average() {
            return (double) totalDuration / invocations;
        }
    }
}
