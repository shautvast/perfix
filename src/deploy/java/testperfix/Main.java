package testperfix;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("Start me with -javaagent:target/agent-0.1-SNAPSHOT.jar -Dperfix.includes=testperfix");
        System.out.println("Then start putty (or other telnet client) and telnet to localhost:2048");
        run();
    }

    public static void run() {
        someJdbcMethod();
        someOtherMethod();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void someJdbcMethod() {
        try {
            Class.forName("org.h2.Driver");
            Connection connection = DriverManager.getConnection("jdbc:h2:mem:default", "sa", "");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select CURRENT_DATE()");
            while (resultSet.next()){
                System.out.println("today is "+resultSet.getObject(1));
            }
        } catch (ClassNotFoundException | SQLException e) {
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
