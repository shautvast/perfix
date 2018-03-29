package perfix;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Agent {

    public static void premain(String agentArgs, Instrumentation inst) {
        List<String> excludes = determineExcludes();

        inst.addTransformer((classLoader, resource, aClass, protectionDomain, uninstrumentedByteCode) -> {
            if (!shouldExclude(resource, excludes).get()) {
                try {
                    return instrumentMethod(resource);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
            }
            return uninstrumentedByteCode;
        });
    }

    private static byte[] instrumentMethod(String resource) throws NotFoundException, IOException, CannotCompileException {
        ClassPool cp = ClassPool.getDefault();
        CtClass methodClass = cp.get("perfix.Method");


        CtClass cc = cp.get(resource.replaceAll("/", "."));
        Arrays.stream(cc.getDeclaredMethods()).forEach(m -> {
            instrumentMethod(methodClass, m);
        });
        byte[] byteCode = cc.toBytecode();
        cc.detach();
        return byteCode;
    }

    private static void instrumentMethod(CtClass methodClass, CtMethod m) {
        try {
            m.addLocalVariable("perfixmethod", methodClass);
            m.insertBefore("perfixmethod = perfix.Method.start(\"" + m.getLongName() + "\");");
            m.insertAfter("perfixmethod.stop();");
        } catch (CannotCompileException e) {
            e.printStackTrace(System.err);
        }
    }

    private static List<String> determineExcludes() {
        List<String> excludes = new ArrayList<>(Arrays.asList(System.getProperty("perfix.excludes").split(",")));
        excludes.add("perfix");
        return excludes;
    }

    private static BooleanWrapper shouldExclude(String resource, List<String> excludes) {
        BooleanWrapper excluded = new BooleanWrapper(false);
        excludes.forEach(exclude -> {
            if (resource.startsWith(exclude)) {
                excluded.set(true);
            }
        });
        return excluded;
    }

    static class BooleanWrapper {
        boolean value;

        public BooleanWrapper(boolean value) {
            this.value = value;
        }

        void set(boolean value) {
            this.value = value;
        }

        boolean get() {
            return value;
        }
    }
}

