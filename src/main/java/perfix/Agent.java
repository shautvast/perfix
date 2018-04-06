package perfix;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class Agent {

    private static final String PORT_PROPERTY = "perfix.port";
    private static final String INCLUDES_PROPERTY = "perfix.includes";

    private static final String DEFAULT_PORT = "2048";
    private static final String MESSAGE = "Perfix agent active";

    private static final String PERFIX_METHOD_CLASS = "perfix.Method";

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println(MESSAGE);

        int port = Integer.parseInt(System.getProperty(PORT_PROPERTY, DEFAULT_PORT));

        instrumentCode(inst);

        new Server().startListeningOnSocket(port);
    }

    private static void instrumentCode(Instrumentation inst) {
        List<String> includes = determineIncludes();

        inst.addTransformer((classLoader, resource, aClass, protectionDomain, uninstrumentedByteCode)
                -> createByteCode(includes, resource, uninstrumentedByteCode));
    }

    private static byte[] createByteCode(List<String> includes, String resource, byte[] uninstrumentedByteCode) {
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
    }

    private static byte[] instrumentMethod(String resource) throws
            NotFoundException, IOException, CannotCompileException {
        ClassPool cp = ClassPool.getDefault();
        CtClass methodClass = cp.get(PERFIX_METHOD_CLASS);

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

