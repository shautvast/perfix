package perfix.instrument;

import javassist.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class JdbcInstrumentor extends Instrumentor {
    private static final Logger log = Logger.getLogger("perfix");

    private static CtClass statementTextClass = null;

    JdbcInstrumentor(List<String> includes, ClassPool classPool) {
        super(includes, classPool);
        try {
            statementTextClass = classpool.get("perfix.instrument.StatementText");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    /* Need to enhance interface to be able to set a statement (string) for perfix. */
    byte[] instrumentJdbcPreparedStatement(CtClass preparedStatementInterface, byte[] uninstrumentedByteCode) {
        try {
            preparedStatementInterface.getDeclaredMethod("setSqlForPerfix");
        } catch (NotFoundException e1) {
            e1.printStackTrace();
            try {
                CtMethod setSqlForPerfix = new CtMethod(CtClass.voidType, "setSqlForPerfix", new CtClass[]{stringClass}, preparedStatementInterface);
                preparedStatementInterface.addMethod(setSqlForPerfix);

            } catch (CannotCompileException e2) {
                e2.printStackTrace();
                return uninstrumentedByteCode;
            }
        }
        try {
            return bytecode(preparedStatementInterface);
        } catch (CannotCompileException | IOException e) {
            log.severe(e.toString());
            return uninstrumentedByteCode;
        }
    }

    /* Prepared statement methods that actually execute sql don't have the statement in their parameters (unlike java.sql.Statement)
     * instead every jdbc vendor has a specific String field in their PreparedStatement impl that contains the statement.
     *
     * To circumvent vendor specifics in perfix, the first argument for prepareStatement (the sql String) is intercepted here
     * and injected into the PreparedStatement instance under a fixed name, whatever the implementation type */
    byte[] instrumentJdbcConnection(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
        try {
            stream(classToInstrument.getDeclaredMethods("prepareStatement")).forEach(method -> {
                        try {
                            // The sql statement String that is the first argument for this method is injected into PreparedStatementImpl
                            // using a name known (only) to perfix, so that it can fetch it later in that class (instance)
                            // this way no JDBC implementor specific code is needed
                            CtClass preparedStatementInterface = classpool.get("java.sql.PreparedStatement");

                            try {
                                preparedStatementInterface.getDeclaredMethod("setSqlForPerfix");
                            } catch (NotFoundException e1) {
                                CtMethod setSqlForPerfix = new CtMethod(CtClass.voidType, "setSqlForPerfix", new CtClass[]{stringClass}, preparedStatementInterface);
                                preparedStatementInterface.addMethod(setSqlForPerfix);
                            }

                            method.insertAfter("$_.setSqlForPerfix($1);"); //$_ is result instance, $1 is first argument
                        } catch (CannotCompileException | NotFoundException e) {
                            log.severe(e.toString());
                        }
                    }
            );
            return bytecode(classToInstrument);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            // suppress
            e.printStackTrace();
        }
        return uninstrumentedByteCode;
    }

    /* */
    byte[] instrumentJdbcPreparedStatementImpl(CtClass preparedStatementClass, byte[] uninstrumentedByteCode) {
        try {
            addPerfixFields(preparedStatementClass);
            addPerfixStatementSetter(preparedStatementClass);
            stream(preparedStatementClass.getDeclaredMethods("execute")).forEach(this::instrumentJdbcCall);
            stream(preparedStatementClass.getDeclaredMethods("executeQuery")).forEach(this::instrumentJdbcCall);
            stream(preparedStatementClass.getDeclaredMethods("executeUpdate")).forEach(this::instrumentJdbcCall);

            getDeclaredMethods(preparedStatementClass, "setString", "setObject", "setDate", "setTime", "setTimestamp")
                    .forEach(method -> {
                                try {
                                    method.insertBefore("perfix.instrument.StatementText.set(_perfixSqlStatement,$1, \"\'\"+$2+\"\'\");");
                                } catch (CannotCompileException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
            getDeclaredMethods(preparedStatementClass,
                    "setInt", "setFloat", "setDouble", "setBoolean", "setLong", "setShort")
                    .forEach(method -> {
                                try {
                                    method.insertBefore("perfix.instrument.StatementText.set(_perfixSqlStatement,$1,$2);");
                                } catch (CannotCompileException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
            getDeclaredMethods(preparedStatementClass, "setNull")
                    .forEach(method -> {
                                try {
                                    method.insertBefore("perfix.instrument.StatementText.set(_perfixSqlStatement,$1,\"[NULL]\");");
                                } catch (CannotCompileException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );

            return bytecode(preparedStatementClass);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            return uninstrumentedByteCode;
        }
    }

    private Stream<CtMethod> getDeclaredMethods(CtClass preparedStatementClass, String... methodnames) {
        List<CtMethod> methods = new ArrayList<>();
        for (String methodname : methodnames) {
            try {
                methods.addAll(Arrays.asList(preparedStatementClass.getDeclaredMethods(methodname)));

            } catch (NotFoundException e) {
                log.severe(e.toString());
                throw new RuntimeException(e);
            }
        }

        return methods.stream();
    }


    byte[] instrumentJdbcStatement(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
        try {
            //instrument executeQuery to record query duration
            stream(classToInstrument.getDeclaredMethods("executeQuery")).forEach(method ->
                    instrumentJdbcCall(method, "$1")
            );
            //instrument executeUpdate to record query duration
            stream(classToInstrument.getDeclaredMethods("executeUpdate")).forEach(method ->
                    instrumentJdbcCall(method, "$1")
            );
            return bytecode(classToInstrument);
        } catch (Exception e) {
            e.printStackTrace();
            return uninstrumentedByteCode;
        }

    }

    private void addPerfixFields(CtClass preparedStatementClass) throws CannotCompileException, NotFoundException {
        // add a String field that will contain the statement
        CtField perfixSqlField = new CtField(statementTextClass, "_perfixSqlStatement", preparedStatementClass);
        perfixSqlField.setModifiers(Modifier.PRIVATE);
        preparedStatementClass.addField(perfixSqlField);
    }

    private void addPerfixStatementSetter(CtClass preparedStatementImplClass) throws CannotCompileException {
        // add setter for the new field
        CtMethod setSqlForPerfix = new CtMethod(CtClass.voidType, "setSqlForPerfix", new CtClass[]{stringClass}, preparedStatementImplClass);
        setSqlForPerfix.setModifiers(Modifier.PUBLIC);
        setSqlForPerfix.setBody("_perfixSqlStatement=new perfix.instrument.StatementText($1);");
        preparedStatementImplClass.addMethod(setSqlForPerfix);
    }

    boolean isJdbcStatementImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith("java/sql")) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals("java.sql.Statement") && !i.getName().equals("java.sql.PreparedStatement"));
        }
        return false;
    }

    boolean isJdbcPreparedStatement(String resource) {
        return resource.equals("java/sql/PreparedStatement");
    }

    boolean isJdbcPreparedStatementImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!ctClass.isInterface() && !resource.startsWith("java/sql")) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals("java.sql.PreparedStatement"));
        }
        return false;
    }

    boolean isJdbcConnectionImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!ctClass.isInterface() && !resource.startsWith("java/sql")) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals("java.sql.Connection"));
        }
        return false;
    }
}
