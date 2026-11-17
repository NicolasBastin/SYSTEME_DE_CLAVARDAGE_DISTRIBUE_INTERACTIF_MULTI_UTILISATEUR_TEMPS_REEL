import java.net.*;
import java.io.*;

public class TCPSender {
    public static void sendMessage(InetAddress targetIp, int targetPort, String message) throws IOException {
        try (Socket socket = new Socket(targetIp, targetPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
            out.flush();
        }
    }
}