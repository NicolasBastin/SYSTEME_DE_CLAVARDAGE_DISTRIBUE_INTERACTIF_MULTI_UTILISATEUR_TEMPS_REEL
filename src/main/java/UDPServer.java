import java.net.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UDPServer implements Runnable {
    private final int port;
    private DatagramSocket socket;
    // CopyOnWriteArrayList évite les bugs si on ajoute un observateur pendant la lecture
    private final List<UDPObserver> observers = new CopyOnWriteArrayList<>();

    public UDPServer(int port) {
        this.port = port;
    }

    public void addObserver(UDPObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port)); // Crash possible si le port est pris
            
            byte[] buffer = new byte[1024];
            while (!socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String rawData = new String(packet.getData(), 0, packet.getLength()).trim();
                String[] parts = rawData.split("\\|");
                
                if (parts.length >= 3) {
                    // On fabrique l'objet UDPMessage
                    UDPMessage msg = new UDPMessage(parts[0], parts[1], parts[2], packet.getAddress(), packet.getPort());
                    
                    // On avertit tous ceux qui écoutent !
                    for (UDPObserver obs : observers) {
                        obs.onMessageReceived(msg);
                    }
                }
            }
        } catch (BindException e) {
            // Le port est déjà utilisé (Local recovery / Error handling), on fait remonter l'erreur.
            throw new RuntimeException("Erreur critique : Le port " + port + " est déjà utilisé par une autre application.", e);
        } catch (IOException e) {
            // Si il y a d'autres erreurs, on les gerera ici
        }
    }
}