package perfix.server;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;

public class SshSessionFactory
        implements CommandFactory, Factory<Command> {

    @Override
    public Command createCommand(String command) {
        return new SshSessionInstance();
    }

    @Override
    public Command create() {
        return createCommand("none");
    }

}