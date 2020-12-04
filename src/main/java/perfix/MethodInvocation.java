package perfix;

/**
 * contains start and stop time for method/query/servlet
 */
public class MethodInvocation {
    private final long timestamp;
    long duration;

    MethodInvocation(String name) {
        timestamp = System.nanoTime();

    }

    public long getDuration() {
        return duration;
    }

    public void registerEndingTime(long t1) {
        duration = t1 - timestamp;
    }
}
