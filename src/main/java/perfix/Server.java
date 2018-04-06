package perfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    void startListeningOnSocket(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            new Thread(() -> {
                for (; ; ) {
                    try {
                        Socket client = serverSocket.accept();

                        PrintStream out = new PrintStream(client.getOutputStream());
                        out.println("press [enter] for report or [q and enter] to quit");

                        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.equals("q")) {
                                try {
                                    client.close();
                                    break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Registry.report(out);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
