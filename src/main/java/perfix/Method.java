package perfix;

public class Method {
    private final long t0;
    private final String name;
    private long t1;

    private Method(String name) {
        t0 = System.nanoTime();
        this.name = name;
    }

    public static Method start(String name) {
        return new Method(name);
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
