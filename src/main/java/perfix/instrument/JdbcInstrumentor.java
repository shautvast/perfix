package perfix.instrument;

import javassist.*;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.stream;

public class JdbcInstrumentor extends Instrumentor {

    private static final String JAVASQL_EXECUTE_METHOD = "execute";
    private static final String JAVASQL_EXECUTEQUERY_METHOD = "executeQuery";
    private static final String JAVASQL_EXECUTEUPDATE_METHOD = "executeUpdate";
    private static final String JAVASQL_PACKAGE = "java/sql";
    private static final String JAVASQL_STATEMENT_INTERFACE = "java.sql.Statement";
    private static final String JAVASQL_PREPAREDSTATEMENT_INTERFACE = "java.sql.PreparedStatement";
    private static final String JAVASQL_CONNECTION_INTERFACE = "java.sql.Connection";
    private static final String JAVASQL_PREPAREDSTATEMENT_RESOURCENAME = "java/sql/PreparedStatement";
    private static final String JAVASQL_PREPARED_STATEMENT_CLASSNAME = "java.sql.PreparedStatement";
    private static final String JAVASQL_PREPARESTATEMENT_METHODNAME = "prepareStatement";

    private static final String PERFIX_SQLSTATEMENT_FIELD = "_perfixSqlStatement";
    private static final String PERFIX_SETSQL_METHOD = "setSqlForPerfix";


    JdbcInstrumentor(List<String> includes, ClassPool classPool) {
        super(includes, classPool);
    }

    /* Need to enhance interface to be able to set a statement (string) for perfix. */
    byte[] instrumentJdbcPreparedStatement(CtClass preparedStatementInterface, byte[] uninstrumentedByteCode) {
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
    byte[] instrumentJdbcConnection(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
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
    byte[] instrumentJdbcPreparedStatementImpl(CtClass preparedStatementClass, byte[] uninstrumentedByteCode) {
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


    byte[] instrumentJdbcStatement(CtClass classToInstrument, byte[] uninstrumentedByteCode) {
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

    boolean isJdbcStatementImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith(JAVASQL_PACKAGE)) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals(JAVASQL_STATEMENT_INTERFACE) && !i.getName().equals(JAVASQL_PREPARED_STATEMENT_CLASSNAME));
        }
        return false;
    }

    boolean isJdbcPreparedStatement(String resource) {
        return resource.equals(JAVASQL_PREPAREDSTATEMENT_RESOURCENAME);
    }

    boolean isJdbcPreparedStatementImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith(JAVASQL_PACKAGE)) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals(JAVASQL_PREPAREDSTATEMENT_INTERFACE));
        }
        return false;
    }

    boolean isJdbcConnectionImpl(String resource, CtClass ctClass) throws NotFoundException {
        if (!resource.startsWith(JAVASQL_PACKAGE)) {
            return stream(ctClass.getInterfaces())
                    .anyMatch(i -> i.getName().equals(JAVASQL_CONNECTION_INTERFACE));
        }
        return false;
    }
}
