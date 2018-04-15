# perfix
Pretty basic profiling tool for JVM's

* agent that instruments loaded classes: -javaagent:perfix.jar
* include classes for instrumentation with -Dperfix.includes=com.project. ...etc (includes subpackages)
* telnet interface to report executed methods:
<br/> * #invocations
<br/> * total execution time for the method in nanoseconds (this is also the sorting order)
<br/> * average time in nanoseconds per method (= total/#invocations)
