package perfix.server;

import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.IOException;
import java.nio.file.Paths;

public class SSHServer implements Server, Runnable {
    private static final String BANNER = "\n\nWelcome to Perfix!\n\n";

    private int port;

    public void startListeningOnSocket(int port) {
        this.port=port;
        new Thread(this).start();
    }

    @Override
    public void run() {
        SshServer sshd = SshServer.setUpDefaultServer();

        PropertyResolverUtils.updateProperty(sshd, ServerFactoryManager.WELCOME_BANNER, BANNER);
        sshd.setPasswordAuthenticator((s, s1, serverSession) -> true);
        sshd.setPort(port);
        sshd.setShellFactory(new SshSessionFactory());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));

        try {
            sshd.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (;;){
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
