package perfix;

import perfix.instrument.Util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Registry {

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
        MethodNode newNode = new MethodNode(name);

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
            methodInvocation.registerEndingTime(System.nanoTime());
        }
        MethodNode methodNode = currentMethod.get();
        methodNode.setInvocation(methodInvocation);

        currentMethod.set(methodNode.parent);
    }

    public static SortedMap<Long, Report> sortedMethodsByDuration() {
        //walk the stack to group methods by their name
        Map<String, List<MethodInvocation>> methods = new ConcurrentHashMap<>();
        collectInvocationsPerMethodName(methods, callstack);

        //gather invocations by method name and calculate statistics
        SortedMap<Long, Report> sortedByTotal = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        methods.forEach((name, measurements) -> {
            long totalDuration = measurements.stream()
                    .filter(Objects::nonNull)
                    .mapToLong(MethodInvocation::getDuration).sum();
            sortedByTotal.put(totalDuration, new Report(name, measurements.size(), totalDuration));
        });
        return sortedByTotal;
    }

    private static void collectInvocationsPerMethodName(Map<String, List<MethodInvocation>> invocations, List<MethodNode> nodes) {
        nodes.forEach(methodNode -> {
            invocations.computeIfAbsent(methodNode.getName(), key -> new ArrayList<>()).add(methodNode.getInvocation());
            collectInvocationsPerMethodName(invocations, methodNode.children);
        });

    }

    public static List<MethodNode> getCallStack() {
        return callstack;
    }

    public static void clear() {
        callstack.clear();
    }
}
