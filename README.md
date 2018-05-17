# perfix
Pretty basic profiling tool for JVM's

# Highlights:
* Provides method and SQL statement execution time.
* Meant for development time (after process stops, data is gone). 
* Minimalistic commandline interface (ssh).
* Execution time is measured in nanoseconds, reported in milliseconds (this way the totals and averages are most precise, but also human readable).
* No manual instrumentation necessary using loadtime bytecode manipulation (javassist).
* No special jdbc configuration necessary (ie no wrapped jdbc driver).
* The agent is also the server (unlike commercial tooling). This way there is no overhead in interprocess communication.
* Minimal memory footprint (agent is 2.5 mb).
* Overhead (in method execution time) not clear yet. I wouldn't use it in production. 

# Usage
* Agent that instruments loaded classes: -javaagent:<path>/perfix.jar
* Include classes for instrumentation with -Dperfix.includes=com.project. ...etc (includes subpackages)
* Ssh interface to report executed methods and sql query excutions:
<br/> * #invocations
<br/> * total execution time for the method in nanoseconds (this is also the sorting order)
<br/> * average time in nanoseconds per method (= total/#invocations)
* The ssh server starts on port 2048 by default. Use -Dperfix.port=... to adjust.


# roadmap
* Finish jdbc query logging
* Make output format configurable
* Implement password login (now any)
* Add web interface (maybe)
* Implement an actual call stack the way commercial tools work

# DISCLAIMER:
This has only been tested on oracle java8 in spring-boot using tomcat web-container
