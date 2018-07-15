package perfix.instrument;

public class Util {
    private final static ThreadLocal<Boolean> STATEMENT_INSTRUMENTED_TL = new ThreadLocal<>();

    public static void startExecution() {
        STATEMENT_INSTRUMENTED_TL.set(true);
    }

    public static void endExecution() {
        STATEMENT_INSTRUMENTED_TL.set(false);
    }

    public static boolean isFirstExecutionStarted() {
        Boolean isStarted = STATEMENT_INSTRUMENTED_TL.get();
        return isStarted != null ? isStarted : false;
    }
}
