package perfix;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.List;

import static java.util.Arrays.stream;

class ClassInstrumentor {

    private static final String JAVA_INNERCLASS_SEPARATOR = "$";
    private static final String JAVA_STRING = "java.lang.String";
    private static final String JAVASQL_PACKAGE = "java/sql";
    private static final String JAVASQL_STATEMENT_INTERFACE = "java.sql.Statement";
    private static final String JAVASQL_EXECUTE_METHOD = "execute";
    private static final String JAVASQL_EXECUTEQUERY_METHOD = "executeQuery";
    private static final String JAVASQL_EXECUTEUPDATE_METHOD = "executeUpdate";
    private static final String JAVASQL_PREPAREDSTATEMENT_INTERFACE = "java.sql.PreparedStatement";
    private static final String JAVASQL_CONNECTION_INTERFACE = "java.sql.Connection";
    private static final String JAVASQL_PREPARED_STATEMENT_CLASSNAME = "java.sql.PreparedStatement";
    private static final String JAVASQL_PREPAREDSTATEMENT_RESOURCENAME = "java/sql/PreparedStatement";
    private static final String JAVASQL_PREPARESTATEMENT_METHODNAME = "prepareStatement";

    private static final String JAVASSIST_FIRST_ARGUMENT_NAME = "$1";
    private static final String JAVASSIST_RETURNVALUE = "$_";

    private static final String PERFIX_METHODINVOCATION_CLASS = "perfix.MethodInvocation";
    private static final String PERFIX_SQLSTATEMENT_FIELD = "_perfixSqlStatement";
    private static final String PERFIX_SETSQL_METHOD = "setSqlForPerfix";

    private ClassPool classpool;
    private List<String> includes;
    private CtClass perfixMethodInvocationClass;
    private CtClass stringClass;

    ClassInstrumentor(List<String> includes) {
        this.includes = includes;
        this.classpool = ClassPool.getDefault();
        try {
            perfixMethodInvocationClass = getCtClass(PERFIX_METHODINVOCATION_CLASS);
            stringClass = classpool.get(JAVA_STRING);

        } catch (NotFoundException e) {
            //suppress TODO implement trace
        }
    }

    void instrumentCode(Instrumentation inst) {
        inst.addTransformer((classLoader, resource, aClass, protectionDomain, uninstrumentedByteCode)
                -> instrumentCodeForClass(includes, resource, uninstrumentedByteCode));
    }

    private byte[] instrumentCodeForClass(List<String> includes, String resource, byte[] uninstrumentedByteCode) {
        if (!isInnerClass(resource)) {
            try {
                CtClass ctClass = getCtClassForResource(resource);

                if (isJdbcStatementImpl(resource, ctClass)) {
                    return instrumentJdbcStatement(ctClass, uninstrumentedByteCode);
                }

                if (isJdbcConnectionImpl(resource, ctClass)) {
                    return instrumentJdbcConnection(ctClass, uninstrumentedByteCode);
                }

                if (isJdbcPreparedStatement(resource)) {
                    return instrumentJdbcPreparedStatement(ctClass, uninstrumentedByteCode);
                }
                if (isJdbcPreparedStatementImpl(resource, ctClass)) {
                    return instrumentJdbcPreparedStatementImpl(ctClass, uninstrumentedByteCode);
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

    /* Need to enhance interface to be able to set a statement (string) for perfix. */
    private byte[] instrumentJdbcPreparedStatement(CtClass preparedStatementInterface, byte[] uninstrumentedByteCode) {
        try {
            preparedStatementInterface.getDeclaredMethod(PERFIX_SETSQL_METHOD);
        } catch (NotFoundException e1) {
            try {
                CtMethod setSqlForPerfix = new CtMethod(CtClass.voidType, PERFIX_SETSQL_METHOD, new CtClass[]{stringClass}, preparedStatementInterface);
                preparedStatementInterface.addMethod(setSqlForPerfix);

            } catch (CannotCompileException e2) {
                return uninstrumentedByteCode;
            }
        }
        try {
            return bytecode(preparedStatementInterface);
        } catch (CannotCompileException | IOException e) {
            return uninstrumentedByteCode;
        }
    }

    /* Prepared statement methods that actually execute sql don't have the statement in their parameters (unlike java.sql.Statement)
     * instead every jdbc vendor has a specific String field in their PreparedStatement impl that contains the statement.
     *
     * To circumvent vendor specifics in perfix, the first argument for prepareStatement (the sql String) is intercepted here
     * and injected into the PreparedStatement instance under a fixed name, whatever the implementation type */
    private byte[] instrumentJdbcConnection(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
        try {
            stream(classToInstrument.getDeclaredMethods(JAVASQL_PREPARESTATEMENT_METHODNAME)).forEach(method -> {
                        try {
                            // The sql statement String that is the first argument for this method is injected into PreparedStatementImpl 
                            // using a name known (only) to perfix, so that it can fetch it later in that class (instance)
                            // this way no JDBC implementor specific code is needed
                            CtClass preparedStatementInterface = classpool.get(JAVASQL_PREPARED_STATEMENT_CLASSNAME);

                            try {
                                preparedStatementInterface.getDeclaredMethod(PERFIX_SETSQL_METHOD);
                            } catch (NotFoundException e1) {
                                CtMethod setSqlForPerfix = new CtMethod(CtClass.voidType, PERFIX_SETSQL_METHOD, new CtClass[]{stringClass}, preparedStatementInterface);
                                preparedStatementInterface.addMethod(setSqlForPerfix);
                            }

                            method.insertAfter(JAVASSIST_RETURNVALUE + "." + PERFIX_SETSQL_METHOD + "(" + JAVASSIST_FIRST_ARGUMENT_NAME + ");"); //$_ is result instance, $1 is first argument
                        } catch (CannotCompileException | NotFoundException e) {
                            // suppress
                            e.printStackTrace();
                        }
                    }
            );
            return bytecode(classToInstrument);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            // suppress
        }
        return uninstrumentedByteCode;
    }

    /* */
    private byte[] instrumentJdbcPreparedStatementImpl(CtClass preparedStatementClass, byte[] uninstrumentedByteCode) {
        try {
            addPerfixStatementField(preparedStatementClass);
            addPerfixStatementSetter(preparedStatementClass);

            // instrument execute to record query duration
            stream(preparedStatementClass.getDeclaredMethods(JAVASQL_EXECUTE_METHOD)).forEach(method ->
                    instrumentMethod(method, PERFIX_SQLSTATEMENT_FIELD)
            );

            // instrument executeQuery to record query duration
            stream(preparedStatementClass.getDeclaredMethods(JAVASQL_EXECUTEQUERY_METHOD)).forEach(method ->
                    instrumentMethod(method, PERFIX_SQLSTATEMENT_FIELD)
            );

            // instrument executeUpdate to record query duration
            stream(preparedStatementClass.getDeclaredMethods(JAVASQL_EXECUTEUPDATE_METHOD)).forEach(method ->
                    instrumentMethod(method, PERFIX_SQLSTATEMENT_FIELD)
            );

            return bytecode(preparedStatementClass);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            return uninstrumentedByteCode;
        }
    }


    private byte[] instrumentJdbcStatement(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
        try {
            //instrument executeQuery to record query duration
            stream(classToInstrument.getDeclaredMethods(JAVASQL_EXECUTEQUERY_METHOD)).forEach(method ->
                    instrumentMethod(method, JAVASSIST_FIRST_ARGUMENT_NAME)
            );
            //instrument executeUpdate to record query duration
            stream(classToInstrument.getDeclaredMethods(JAVASQL_EXECUTEUPDATE_METHOD)).forEach(method ->
                    instrumentMethod(method, JAVASSIST_FIRST_ARGUMENT_NAME)
            );
            return bytecode(classToInstrument);
        } catch (Exception e) {
            return uninstrumentedByteCode;
        }

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

    private CtClass getCtClass(String classname) throws NotFoundException {
        return classpool.get(classname);
    }

    private void instrumentMethod(CtMethod methodToinstrument) {
        instrumentMethod(methodToinstrument, "\"" + methodToinstrument.getLongName() + "\"");
    }

    /* record times at beginning and end of method body*/
    private void instrumentMethod(CtMethod methodToinstrument, String metricName) {
        try {
            methodToinstrument.addLocalVariable("_perfixmethod", perfixMethodInvocationClass);
            methodToinstrument.insertBefore("_perfixmethod = perfix.MethodInvocation.start(" + metricName + ");");
            methodToinstrument.insertAfter("perfix.MethodInvocation.stop(_perfixmethod);");
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
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

    private boolean isJdbcStatementImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith(JAVASQL_PACKAGE)) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals(JAVASQL_STATEMENT_INTERFACE) && !i.getName().equals(JAVASQL_PREPARED_STATEMENT_CLASSNAME));
        }
        return false;
    }

    private boolean isJdbcPreparedStatement(String resource) {
        return resource.equals(JAVASQL_PREPAREDSTATEMENT_RESOURCENAME);
    }

    private boolean isJdbcPreparedStatementImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith(JAVASQL_PACKAGE)) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals(JAVASQL_PREPAREDSTATEMENT_INTERFACE));
        }
        return false;
    }

    private boolean isJdbcConnectionImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith(JAVASQL_PACKAGE)) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals(JAVASQL_CONNECTION_INTERFACE));
        }
        return false;
    }

    private byte[] bytecode(CtClass classToInstrument) throws IOException, CannotCompileException {
        classToInstrument.detach();
        return classToInstrument.toBytecode();
    }

    private void addPerfixStatementField(CtClass preparedStatementClass) throws CannotCompileException {
        // add a String field that will contain the statement
        CtField perfixSqlField = new CtField(stringClass, PERFIX_SQLSTATEMENT_FIELD, preparedStatementClass);
        perfixSqlField.setModifiers(Modifier.PRIVATE);
        preparedStatementClass.addField(perfixSqlField);
    }

    private void addPerfixStatementSetter(CtClass preparedStatementImplClass) throws CannotCompileException {
        // add setter for the new field 
        CtMethod setSqlForPerfix = new CtMethod(CtClass.voidType, PERFIX_SETSQL_METHOD, new CtClass[]{stringClass}, preparedStatementImplClass);
        setSqlForPerfix.setModifiers(Modifier.PUBLIC);
        setSqlForPerfix.setBody(PERFIX_SQLSTATEMENT_FIELD + "=" + JAVASSIST_FIRST_ARGUMENT_NAME + ";");
        preparedStatementImplClass.addMethod(setSqlForPerfix);
    }
}
