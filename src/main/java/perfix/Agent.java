package perfix;

import javassist.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Agent {

    public static final String MESSAGE = "Perfix agent active";

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println(MESSAGE);

        startListeningOnSocket();

        instrumentCode(inst);
    }

    private static void instrumentCode(Instrumentation inst) {
        List<String> includes = determineIncludes();

        inst.addTransformer((classLoader, resource, aClass, protectionDomain, uninstrumentedByteCode) -> {
            if (!isInnerClass(resource) && shouldInclude(resource, includes)) {
                try {
                    byte[] instrumentedBytecode = instrumentMethod(resource);
                    if (instrumentedBytecode != null) {
                        return instrumentedBytecode;
                    }
                } catch (Exception ex) {
                    //suppress
                }
            }
            return uninstrumentedByteCode;
        });
    }

    private static void startListeningOnSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(2048);
            new Thread(() -> {
                for (; ; ) {
                    try {
                        Socket client = serverSocket.accept();

                        PrintStream out = new PrintStream(client.getOutputStream());
                        out.println("press [enter] for report or [q and enter] to quit");

                        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.equals("q")) {
                                try {
                                    client.close();
                                    break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Registry.report(out);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } 
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isInnerClass(String resource) {
        return resource.contains("$");
    }

    private static byte[] instrumentMethod(String resource) throws
            NotFoundException, IOException, CannotCompileException {
        ClassPool cp = ClassPool.getDefault();
        CtClass methodClass = cp.get("perfix.Method");

        CtClass classToInstrument = cp.get(resource.replaceAll("/", "."));
        if (!classToInstrument.isInterface()) {
            Arrays.stream(classToInstrument.getDeclaredMethods()).forEach(m -> {
                instrumentMethod(methodClass, m);
            });
            byte[] byteCode = classToInstrument.toBytecode();
            classToInstrument.detach();
            return byteCode;
        } else {
            return null;
        }
    }

    private static void instrumentMethod(CtClass methodClass, CtMethod m) {
        try {
            m.addLocalVariable("perfixmethod", methodClass);
            m.insertBefore("perfixmethod = perfix.Method.start(\"" + m.getLongName() + "\");");
            m.insertAfter("perfixmethod.stop();");
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> determineIncludes() {
        return new ArrayList<>(Arrays.asList(System.getProperty("perfix.includes").split(",")));
    }

    private static boolean shouldInclude(String resource, List<String> excludes) {
        BooleanWrapper included = new BooleanWrapper(false);
        excludes.forEach(include -> {
            if (resource.startsWith(include)) {
                included.set(true);
            }
        });
        return included.get();
    }

    static class BooleanWrapper {
        boolean value;

        BooleanWrapper(boolean value) {
            this.value = value;
        }

        void set(boolean value) {
            this.value = value;
        }

        boolean get() {
            return value;
        }

        @Override
        public String toString() {
            return Boolean.toString(value);
        }
    }
}

