package perfix;

public class MethodInvocation {
    private final long t0;
    private final String name;
    private long t1;

    private MethodInvocation(String name) {
        t0 = System.nanoTime();
        if (name != null) {
            this.name = name;
        } else {
            this.name = "<error occurred>";
        }
    }

    public static MethodInvocation start(String name) {
        return new MethodInvocation(name);
    }

    public static void stop(MethodInvocation methodInvocation) {
        methodInvocation.stop();
    }

    public void stop() {
        t1 = System.nanoTime();
        Registry.add(this);
    }

    String getName() {
        return name;
    }

    long getDuration() {
        return t1 - t0;
    }
}
