package perfix.server;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import perfix.Registry;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class SshSessionInstance implements Command, Runnable {

    private static final String ANSI_NEWLINE_CRLF = "\u001B[20h";

    private InputStream input;
    private OutputStream output;
    private ExitCallback callback;

    @Override
    public void start(Environment env) {
        new Thread(this, "PerfixShell").start();
    }

    @Override
    public void run() {
        try {
            output.write("press [enter] for report or [q] to quit\n".getBytes());
            output.write((ANSI_NEWLINE_CRLF).getBytes());
            output.flush();

            boolean exit = false;
            while (!exit) {
                char c = (char) input.read();
                if (c == 'q') {
                    exit = true;
                } else if (c == '\n') {
                    Registry.report(new PrintStream(output));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            callback.onExit(0);
        }
    }

    @Override
    public void destroy() {    }

    @Override
    public void setErrorStream(OutputStream errOS) {
    }

    @Override
    public void setExitCallback(ExitCallback ec) {
        callback = ec;
    }

    @Override
    public void setInputStream(InputStream is) {
        this.input = is;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        this.output = os;
    }

}


