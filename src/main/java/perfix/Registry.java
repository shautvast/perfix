package perfix;

import perfix.instrument.Util;

import java.sql.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles start and stop of method invocations. Measures method/sql/url invocations. Stores individual measurements in sqlite.
 */
@SuppressWarnings("unused") //used from instrumented bytecode
public class Registry {

    private static final ThreadLocal<MethodNode> currentMethod = new ThreadLocal<>();
    private static final String INSERT_REPORT = "insert into report (thread, id, parent_id, invocation_id, timestamp, name, duration) values (?,?,?,?,?,?,?)";
    private static final String CREATE_TABLE = "create table report(thread varchar(255), id int, parent_id int, invocation_id int, timestamp int, name varchar(255), duration integer)";
    private static final String SElECT_TABLE = "SELECT name FROM sqlite_master WHERE type='table' AND name='report';";

    private static BlockingQueue<MethodNode> queue = new LinkedBlockingQueue<>();
    private static Connection connection;

    static {
        initWorker();
        initDatabase();
    }

    private static void initWorker() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            while (true) {
                MethodNode methodNode = queue.take();

                store(methodNode);
            }
        });
    }

    private static void initDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + getSqliteFile());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SElECT_TABLE);
            if (!resultSet.next()) {
                statement.execute(CREATE_TABLE);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getSqliteFile() {
        String dbFileName = System.getProperty(Agent.DBFILE_PROPERTY);
        if (dbFileName == null) {
            return Agent.DEFAULT_DBFILE;
        } else {
            return dbFileName;
        }
    }

    public static MethodNode startJdbc(String name) {
        if (!Util.isFirstExecutionStarted()) {
            Util.startExecution();
            return start(name);
        } else {
            return null;
        }
    }

    public static MethodNode start(String name) {
        MethodNode newNode = new MethodNode(name);

        MethodNode parent = currentMethod.get();
        if (parent != null) {
            newNode.setParent(parent);
            newNode.setInvocationId(parent.getInvocationId());
        }

        currentMethod.set(newNode);
        return newNode;
    }


    @SuppressWarnings("unused")
    public static void stopJdbc() {
        MethodNode current = currentMethod.get();
        if (Util.isFirstExecutionStarted() && current != null) {
            stop();
            Util.endExecution();
        }
    }

    @SuppressWarnings("unused")
    public static void stop() {
        MethodNode current = currentMethod.get();
        if (current != null) {
            current.registerEndingTime(System.nanoTime());
            queue.add(current);
            currentMethod.set(current.getParent());
        }
    }

    private static void store(MethodNode methodNode) {
        try {
            PreparedStatement statement = connection.prepareStatement(INSERT_REPORT);
            statement.setString(1, methodNode.getThreadName());
            statement.setLong(2, methodNode.getId());
            statement.setLong(3, methodNode.getParentId());
            statement.setLong(4, methodNode.getInvocationId());
            statement.setLong(5, methodNode.getTimestamp());
            statement.setString(6, methodNode.getName());
            statement.setLong(7, methodNode.getDuration());
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
