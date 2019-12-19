package perfix.instrument;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.util.List;

import static java.util.Arrays.stream;

public class ServletInstrumentor extends Instrumentor {

    private static final String JAVA_SERVLET_SERVICE_METHOD = "service";
    private static final String JAVA_SERVLET_CLASS = "javax/servlet/http/HttpServlet";

    ServletInstrumentor(List<String> includes, ClassPool classPool) {
        super(includes, classPool);
    }

    public boolean isServletImpl(String resource) {
        return resource.equals(JAVA_SERVLET_CLASS);
    }

    public byte[] instrumentServlet(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
        try {
            stream(classToInstrument.getDeclaredMethods(JAVA_SERVLET_SERVICE_METHOD)).forEach(methodToInstrument -> {
                try {
                    methodToInstrument.insertBefore("perfix.Registry.start($1.getRequestURI());");
                    methodToInstrument.insertAfter("perfix.Registry.stop();");
                } catch (CannotCompileException e) {
                    // ignore and return uninstrumented bytecode
                }
            });
            return bytecode(classToInstrument);
        } catch (Exception e) {
            return uninstrumentedByteCode;
        }
    }
}
