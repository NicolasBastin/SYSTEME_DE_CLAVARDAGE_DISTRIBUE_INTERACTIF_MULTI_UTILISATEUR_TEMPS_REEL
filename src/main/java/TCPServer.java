import java.net.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCPServer implements Runnable {
    private final int port;
    private ServerSocket serverSocket;
    private final List<TCPObserver> observers = new CopyOnWriteArrayList<>();

    public TCPServer(int port) {
        this.port = port;
    }

    public void addObserver(TCPObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                
                try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // On notifie l'application !
                        for (TCPObserver obs : observers) {
                            obs.onPrivateMessageReceived(clientIp, message);
                        }
                    }
                }
                clientSocket.close();
            }
        } catch (IOException e) {
            // Gestion d'erreur
        }
    }
}