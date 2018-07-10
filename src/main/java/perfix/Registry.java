package perfix;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

public class Registry {

    private static final Map<String, List<MethodInvocation>> methods = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> callstack = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentMethod = new ThreadLocal<>();

    @SuppressWarnings("unused")
    public static MethodInvocation start(String name) {
        MethodInvocation methodInvocation = new MethodInvocation(name);
        String parent = currentMethod.get();
        if (parent != null) {
            callstack.computeIfAbsent(parent, k -> new HashSet<>()).add(methodInvocation.getName());
        }
        currentMethod.set(methodInvocation.getName());
        return methodInvocation;
    }


    @SuppressWarnings("unused")
    public static void stop(MethodInvocation methodInvocation) {
        methodInvocation.t1 = System.nanoTime();
        methods.computeIfAbsent(methodInvocation.getName(), key -> new ArrayList<>()).add(methodInvocation);
    }

    public static SortedMap<Long, Report> sortedMethodsByDuration() {
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

    //work in progress
    public static Map<String, Set<Report>> getCallStack() {
        callstack.forEach((name, children) -> {

        });
        return null;
    }

}
