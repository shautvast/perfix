package testperfix.cmdline;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        System.out.println("Perfix Test Application is running. Make sure the agent is active.");
        String includesProperty = System.getProperty("perfix.includes");
        if (includesProperty == null || !includesProperty.equals("testperfix")) {
            System.out.println("Start me with -javaagent:target/agent-0.1-SNAPSHOT.jar -Dperfix.includes=testperfix");

            System.out.println("Exiting now");
            System.exit(0);
        }
        run();
    }

    private static void run() {
        try {
            Class.forName("org.h2.Driver");
            someJdbcStatentMethod();
            someJdbcPreparedStatementMethod();
            someOtherMethod();
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void someJdbcStatentMethod() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:h2:mem:default", "sa", "");
            Statement statement = connection.createStatement();
            statement.executeQuery("select CURRENT_DATE() -- simple statement");
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void someJdbcPreparedStatementMethod() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:h2:mem:default", "sa", "");
            Statement statement = connection.createStatement();
            statement.execute("create table t (v varchar(2))");
            statement.close();
            PreparedStatement preparedStatement = connection.prepareStatement("select * from t where v=? -- prepared statement");
            preparedStatement.setDate(1, new Date(new java.util.Date().getTime()));
            preparedStatement.executeQuery();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void someOtherMethod() {
        try {
            TimeUnit.NANOSECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
