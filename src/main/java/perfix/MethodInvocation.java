package perfix;

/**
 * contains start and stop time for method/query/servlet
 */
public class MethodInvocation {
    private final long t0;
    private final String name;
    long t1;

    MethodInvocation(String name) {
        t0 = System.nanoTime();
        if (name != null) {
            this.name = name;
        } else {
            this.name = "<error occurred>";
        }
    }

    String getName() {
        return name;
    }

    long getDuration() {
        return t1 - t0;
    }

}
