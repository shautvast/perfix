# perfix
Pretty basic profiling tool for JVM's

# Highlights:
* Meant for development time (after process stops, data is gone). 
* Minimal memory footprint (agent < 1 mb).
* Easy setup (2 commandline arguments for java process)
* Minimalistic web interface.
* Execution time is measured in nanoseconds
* No manual instrumentation necessary using loadtime bytecode manipulation (javassist).
* No special jdbc configuration necessary (ie no wrapped jdbc driver).
* The agent is also the server (unlike commercial tooling). This way there is no overhead in interprocess communication.

# Usage
* Agent that instruments loaded classes: -javaagent:<path>/perfix.jar
* Include classes for instrumentation with -Dperfix.includes=com.project. ...etc (includes subpackages)
* Web page to report executed methods and sql query excutions:
<br/> * #invocations
<br/> * total execution time for the method in nanoseconds (this is also the sorting order)
<br/> * average time in nanoseconds per method (= total/#invocations)
* The http server starts on port 2048 by default. Use -Dperfix.port=... to adjust.


# roadmap
* Overhead (in method execution time) not clear yet. I wouldn't use it in production. 
* Finish jdbc query logging (CallableStatement)
* Implement an actual call stack the way commercial tools work
* Ability to dynamically turn off metrics to minimize cpu and memory overhead (when response time is below a set treshold)

# DISCLAIMER:
This has only been tested on oracle java8 in spring-boot using tomcat web-container
