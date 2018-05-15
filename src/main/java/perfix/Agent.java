package perfix;

import javassist.*;
import perfix.server.SSHServer;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

public class Agent {

    private static final String PORT_PROPERTY = "perfix.port";
    private static final String INCLUDES_PROPERTY = "perfix.includes";

    private static final String DEFAULT_PORT = "2048";
    private static final String MESSAGE = "Perfix agent active";

    private static final String PERFIX_METHODINVOCATION_CLASS = "perfix.MethodInvocation";

    private static ClassPool classpool;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println(MESSAGE);

        int port = Integer.parseInt(System.getProperty(PORT_PROPERTY, DEFAULT_PORT));

        classpool = ClassPool.getDefault();

        instrumentCode(inst);

        new SSHServer().startListeningOnSocket(port);
    }

    private static void instrumentCode(Instrumentation inst) {
        List<String> includes = determineIncludes();

        inst.addTransformer((classLoader, resource, aClass, protectionDomain, uninstrumentedByteCode)
                -> createByteCode(includes, resource, uninstrumentedByteCode));
    }

    private static byte[] createByteCode(List<String> includes, String resource, byte[] uninstrumentedByteCode) {
        if (!isInnerClass(resource)) {
            try {
                CtClass ctClass = getCtClassForResource(resource);
                if (isJdbcStatement(resource, ctClass)) {
                    return instrumentJdbcCalls(ctClass);
                }
                if (shouldInclude(resource, includes)) {
                    byte[] instrumentedBytecode = instrumentMethod(ctClass);
                    if (instrumentedBytecode != null) {
                        return instrumentedBytecode;
                    }
                }
            } catch (Exception ex) {
                //suppress
            }

        }
        return uninstrumentedByteCode;
    }

    private static byte[] instrumentJdbcCalls(CtClass classToInstrument) throws IOException, CannotCompileException {
        try {
            stream(classToInstrument.getDeclaredMethods("executeQuery")).forEach(m -> {
                try {
                    m.insertBefore("System.out.println($1);");
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] byteCode = classToInstrument.toBytecode();
        classToInstrument.detach();
        return byteCode;
    }

    private static boolean isJdbcStatement(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith("java/sql")) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals("java.sql.Statement") && !i.getName().equals("java.sql.PreparedStatement"));
        }
        return false;
    }

    private static byte[] instrumentMethod(CtClass classToInstrument) throws
            NotFoundException, IOException, CannotCompileException {

        CtClass perfixMethodInvocationClass = getCtClass(PERFIX_METHODINVOCATION_CLASS);

        if (!classToInstrument.isInterface()) {
            stream(classToInstrument.getDeclaredMethods()).forEach(m -> {
                instrumentMethod(perfixMethodInvocationClass, m);
            });
            byte[] byteCode = classToInstrument.toBytecode();
            classToInstrument.detach();
            return byteCode;
        } else {
            return null;
        }
    }

    private static CtClass getCtClassForResource(String resource) throws NotFoundException {
        return getCtClass(resource.replaceAll("/", "."));
    }

    private static CtClass getCtClass(String classname) throws NotFoundException {
        return classpool.get(classname);
    }

    private static void instrumentMethod(CtClass methodClass, CtMethod methodToinstrument) {
        try {
            methodToinstrument.addLocalVariable("perfixmethod", methodClass);
            methodToinstrument.insertBefore("perfixmethod = perfix.MethodInvocation.start(\"" + methodToinstrument.getLongName() + "\");");
            methodToinstrument.insertAfter("perfixmethod.stop();");
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> determineIncludes() {
        return new ArrayList<>(asList(System.getProperty(INCLUDES_PROPERTY).split(",")));
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

    private static boolean isInnerClass(String resource) {
        return resource.contains("$");
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

