package testperfix.cmdline;

import org.apache.commons.dbcp2.BasicDataSource;

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
            someJdbcStatementMethod();
            someJdbcPreparedStatementMethod();
            someOtherMethod(0);
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void someJdbcStatementMethod() {
        try {
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:default");
            dataSource.setUsername("sa");
            dataSource.setPassword("");

            Connection connection = dataSource.getConnection();
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

    private static void someOtherMethod(int level) {
        try {
            TimeUnit.NANOSECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (level < 10) {
            someOtherMethod(level + 1);
        }
    }
}
