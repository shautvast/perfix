package perfix;

import perfix.instrument.Util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

public class Registry {

    private static final Map<String, List<MethodInvocation>> methods = new ConcurrentHashMap<>();
    private static final List<MethodNode> callstack = new ArrayList<>();
    private static final ThreadLocal<MethodNode> currentMethod = new ThreadLocal<>();

    @SuppressWarnings("unused") //used in generated code
    public static MethodInvocation startJdbc(String name) {
        if (!Util.isFirstExecutionStarted()) {
            Util.startExecution();
            return start(name);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static MethodInvocation start(String name) {
        MethodInvocation methodInvocation = new MethodInvocation(name);
        MethodNode newNode = new MethodNode(methodInvocation.getName());

        MethodNode parent = currentMethod.get();
        if (parent != null) {
            parent.addChild(newNode);
            newNode.parent = parent;
        } else {
            callstack.add(newNode);
        }

        currentMethod.set(newNode);
        return methodInvocation;
    }


    @SuppressWarnings("unused")
    public static void stopJdbc(MethodInvocation queryInvocation) {
        if (Util.isFirstExecutionStarted() && queryInvocation != null) {
            stop(queryInvocation);
            Util.endExecution();
        }
    }

    @SuppressWarnings("unused")
    public static void stop(MethodInvocation methodInvocation) {
        if (methodInvocation != null) {
            methodInvocation.t1 = System.nanoTime();
            methods.computeIfAbsent(methodInvocation.getName(), key -> new ArrayList<>()).add(methodInvocation);
        }
        MethodNode methodNode = currentMethod.get();
        if (methodNode != null) {
            currentMethod.set(methodNode.parent);
        }
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

    public static List<MethodNode> getCallStack() {
        addReport(callstack);
        return callstack;
    }

    private static void addReport(List<MethodNode> callstack) {
        callstack.forEach(methodNode -> {
            LongAdder totalDuration = new LongAdder();
            List<MethodInvocation> methodInvocations = methods.get(methodNode.name);
            methodInvocations.forEach(methodInvocation -> totalDuration.add(methodInvocation.getDuration()));
            methodNode.report = new Report(methodNode.name, methodInvocations.size(), totalDuration.longValue());
            addReport(methodNode.children);
        });
    }

    public static void clear() {
        methods.clear();
        callstack.clear();
    }
}
