package testperfix;

import perfix.Registry;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.print("start me with -javaagent:target/agent-0.1-SNAPSHOT.jar");
        System.out.println(" and preferrably: -Dperfix.excludes=com,java,sun,org");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Registry.report()));
        run();
    }

    public static void run() {
        someOtherMethod();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void someOtherMethod() {
        try {
            TimeUnit.NANOSECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
