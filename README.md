# perfix
Pretty basic profiling tool for JVM's

* agent that instruments loaded classes: -javaagent:perfix.jar
* include classes for instrumentation with -Dperfix.includes=com.project. ...etc (includes subpackages)
* ssh interface to report executed methods:
<br/> * #invocations
<br/> * total execution time for the method in nanoseconds (this is also the sorting order)
<br/> * average time in nanoseconds per method (= total/#invocations)
* The server starts on port 2048 by default. Use -Dperfix.port=... to adjust.

# roadmap
* finish jdbc query logging
* make output format configurable
* implement password login (now any)
