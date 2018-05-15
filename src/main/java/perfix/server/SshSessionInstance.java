package perfix.server;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import perfix.Registry;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class SshSessionInstance implements Command, Runnable {

    private static final String ANSI_LOCAL_ECHO = "\u001B[12l";
    private static final String ANSI_NEWLINE_CRLF = "\u001B[20h";

    private InputStream is;
    private OutputStream os;

    private ExitCallback callback;
    private Thread sshThread;

    @Override
    public void start(Environment env) {
        sshThread = new Thread(this, "EchoShell");
        sshThread.start();
    }

    @Override
    public void run() {
        try {
            os.write("press [enter] for report or [q] to quit\n".getBytes());
            os.write((ANSI_LOCAL_ECHO + ANSI_NEWLINE_CRLF).getBytes());
            os.flush();

            boolean exit = false;
            while (!exit) {
                char c = (char) is.read();
                if (c == 'q') {
                    exit = true;
                } else if (c == '\n') {
                    Registry.report(new PrintStream(os));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            callback.onExit(0);
        }
    }

    @Override
    public void destroy() {
        sshThread.interrupt();
    }

    @Override
    public void setErrorStream(OutputStream errOS) {
    }

    @Override
    public void setExitCallback(ExitCallback ec) {
        callback = ec;
    }

    @Override
    public void setInputStream(InputStream is) {
        this.is = is;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        this.os = os;
    }

}


