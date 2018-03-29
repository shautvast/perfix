# perfix
poor man's profiling for JVM's

* agent that instruments loaded classes: -javaagent:perfix.jar
* skip instrumentation with -Dperfix.excludes=java,com,org ...etc
* shutdown hook to report executed methods:
** #invocations
** total execution time for the method in nanoseconds
** average time in nanoseconds per method (= total/#invocatons)
