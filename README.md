# perfix
Pretty basic profiling tool for JVM's

__Screenshot__
![screenshot](https://github.com/shautvast/perfix/blob/master/screenshot.png)

# Highlights:
* Meant for development time (after process stops, data is gone). 
* Minimal memory footprint (agent ~ 900 kb).
* Easy setup (2 commandline arguments for java process)
* Minimalistic web interface.
* Execution time is measured in nanoseconds
* No manual instrumentation necessary using loadtime bytecode manipulation (javassist).
* No special jdbc configuration necessary (ie no wrapped jdbc driver).
* The agent is also the server (ui backend). 

# Usage
* build the project using: mvn clean install
* copy perfix-agent-$VERSION.jar to <path>/perfix.jar
* Configure your JVM: -javaagent:<path>/perfix.jar
* Include classes for instrumentation with -Dperfix.includes=com.project. ...etc (includes subpackages)
* Head to http://localhost:2048 for reports executed methods and sql query excutions:
<br/> * #invocations
<br/> * total execution time for the method in nanoseconds (this is also the sorting order)
<br/> * average time in nanoseconds per method (= total/#invocations)
* The backend starts on port 2048 by default. Use -Dperfix.port=... to adjust.
* Start the UI using npm install followed by npm start. the UI starts on port 3000 by default.


# roadmap
* Overhead (in method execution time) not clear yet. I wouldn't use it in production. 
* Finish jdbc query logging (CallableStatement)
* Ability to dynamically turn off metrics to minimize cpu and memory overhead (when response time is below a set treshold)
* extend the user interface

# DISCLAIMER:
This has only been tested on oracle java8 in spring-boot using tomcat web-container (and apache dbcp)

Javassist raises the following error:
```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by javassist.util.proxy.SecurityActions (file:/Users/Shautvast/.m2/repository/org/javassist/javassist/3.26.0-GA/javassist-3.26.0-GA.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of javassist.util.proxy.SecurityActions
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```
But it works at least up until java 15.

I cannot fix this issue, but I'm working to replace javassist as a dependency.