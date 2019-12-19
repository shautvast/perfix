package perfix.instrument;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import perfix.MutableBoolean;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;

import static java.util.Arrays.stream;

public class ClassInstrumentor extends Instrumentor {

    private static final String JAVA_INNERCLASS_SEPARATOR = "$";

    JdbcInstrumentor jdbcInstrumentor;
    ServletInstrumentor servletInstrumentor;

    ClassInstrumentor(List<String> includes, ClassPool classPool) {
        super(includes, classPool);
        try {
            stringClass = classpool.get(JAVA_STRING);

        } catch (NotFoundException e) {
            //suppress TODO implement trace
        }
    }

    @Override
    public void instrumentCode(Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return instrumentCodeForClass(includes, className, classfileBuffer);
            }
        });
    }

    private byte[] instrumentCodeForClass(List<String> includes, String resource, byte[] uninstrumentedByteCode) {
        if (!isInnerClass(resource)) {
            try {
                CtClass ctClass = getCtClassForResource(resource);

                if (servletInstrumentor.isServletImpl(resource)) {
                    return servletInstrumentor.instrumentServlet(ctClass, uninstrumentedByteCode);
                }

                if (jdbcInstrumentor.isJdbcStatementImpl(resource, ctClass)) {
                    return jdbcInstrumentor.instrumentJdbcStatement(ctClass, uninstrumentedByteCode);
                }

                if (jdbcInstrumentor.isJdbcConnectionImpl(resource, ctClass)) {
                    return jdbcInstrumentor.instrumentJdbcConnection(ctClass, uninstrumentedByteCode);
                }

                if (jdbcInstrumentor.isJdbcPreparedStatement(resource)) {
                    return jdbcInstrumentor.instrumentJdbcPreparedStatement(ctClass, uninstrumentedByteCode);
                }
                if (jdbcInstrumentor.isJdbcPreparedStatementImpl(resource, ctClass)) {
                    return jdbcInstrumentor.instrumentJdbcPreparedStatementImpl(ctClass, uninstrumentedByteCode);
                }
                if (shouldInclude(resource, includes)) {
                    return instrumentMethods(ctClass, uninstrumentedByteCode);
                }
            } catch (Exception ex) {
                //suppress
            }
        }
        return uninstrumentedByteCode;
    }

    /* for regular classes that require instrumentation instrument all methods to record duration*/
    private byte[] instrumentMethods(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
        if (!classToInstrument.isInterface()) {
            stream(classToInstrument.getDeclaredMethods()).forEach(this::instrumentMethod);

            try {
                return bytecode(classToInstrument);
            } catch (IOException | CannotCompileException e) {
                //suppress
            }
        }
        return uninstrumentedByteCode;

    }

    private CtClass getCtClassForResource(String resource) throws NotFoundException {
        return getCtClass(resource.replaceAll("/", "."));
    }

    private boolean shouldInclude(String resource, List<String> excludes) {
        MutableBoolean included = new MutableBoolean(false);
        excludes.forEach(include -> {
            if (resource.startsWith(include)) {
                included.set(true);
            }
        });
        return included.get();
    }

    private boolean isInnerClass(String resource) {
        return resource.contains(JAVA_INNERCLASS_SEPARATOR);
    }


}
