package perfix.instrument;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.List;

public abstract class Instrumentor {
    static final String JAVA_STRING = "java.lang.String";
    static final String JAVA_HASHMAP = "java.util.HashMap";
    static final String JAVASSIST_FIRST_ARGUMENT_NAME = "$1";
    static final String JAVASSIST_RETURNVALUE = "$_";

    final ClassPool classpool;
    final List<String> includes;
    protected CtClass stringClass;
    protected CtClass hashMapClass;

    Instrumentor(List<String> includes, ClassPool classPool) {
        this.includes = includes;
        this.classpool = classPool;

        try {
            stringClass = classpool.get(JAVA_STRING);
            hashMapClass = classPool.get(JAVA_HASHMAP);

        } catch (NotFoundException e) {
            e.printStackTrace();
            //suppress TODO implement trace
        }
    }

    public static Instrumentor create(List<String> includes) {
        ClassPool classPool = ClassPool.getDefault();
        ClassInstrumentor classInstrumentor = new ClassInstrumentor(includes, classPool);
        classInstrumentor.jdbcInstrumentor = new JdbcInstrumentor(includes, classPool);
        classInstrumentor.servletInstrumentor = new ServletInstrumentor(includes, classPool);
        return classInstrumentor;
    }

    public void instrumentCode(Instrumentation inst) {
    }

    void instrumentMethod(CtMethod methodToinstrument) {
        instrumentMethod(methodToinstrument, "\"" + methodToinstrument.getLongName() + "\"");
    }

    /* record times at beginning and end of method body*/
    void instrumentMethod(CtMethod methodToinstrument, String metricName) {
        try {
            methodToinstrument.insertBefore("perfix.Registry.start(" + metricName + ");");
            methodToinstrument.insertAfter("perfix.Registry.stop();");
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }

    /* record times at beginning and end of sql call
     * sql calls are special in the sense that certain jdbc connection pool implementations
     * create a nested chain of jdbc statement implementations, resulting in too many reported
     * (measured) calls if not handled in a way to prevent this */
    void instrumentJdbcCall(CtMethod methodToinstrument, String metricName) {
        try {
            methodToinstrument.insertBefore("perfix.Registry.startJdbc(" + metricName + ");");
            methodToinstrument.insertAfter("perfix.Registry.stopJdbc();");
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }

    /* record times at beginning and end of method body*/
    void instrumentJdbcCall(CtMethod methodToinstrument) {
        try {
            methodToinstrument.insertBefore("perfix.Registry.startJdbc(perfix.instrument.StatementText.toString(_perfixSqlStatement));");
            methodToinstrument.insertAfter("perfix.Registry.stopJdbc();");
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }

    byte[] bytecode(CtClass classToInstrument) throws IOException, CannotCompileException {
        classToInstrument.detach();
        return classToInstrument.toBytecode();
    }

    CtClass getCtClass(String classname) throws NotFoundException {
        return classpool.get(classname);
    }
}
