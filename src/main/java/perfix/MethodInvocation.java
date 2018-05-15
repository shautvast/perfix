package perfix;

public class MethodInvocation {
    private final long t0;
    private final String name;
    private long t1;

    private MethodInvocation(String name) {
        t0 = System.nanoTime();
        this.name = name;
    }

    public static MethodInvocation start(String name) {
        return new MethodInvocation(name);
    }

    public void stop() {
        t1 = System.nanoTime();
        Registry.add(this);
    }

    public String getName() {
        return name;
    }

    long getDuration() {
        return t1 - t0;
    }
}