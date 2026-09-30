import java.net.*;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class DiscoveryService {
    private static final int DISCOVERY_PORT = 8298;
    private DatagramSocket socket;

    private String myId;
    private String myNickname;
    private List<String> listName = new CopyOnWriteArrayList<>();

    public List<String> getListName() {
        return listName;
    }

    public DiscoveryService(String nickname) {
        this.myNickname = nickname;
        this.myId = "ID_" + System.currentTimeMillis();
    }

    public void startService() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(DISCOVERY_PORT));
            socket.setBroadcast(true);

            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.start();

            System.out.println("Service de découverte démarré. Écoute sur le port " + DISCOVERY_PORT);

            broadcastPresence();

        } catch (SocketException e) {
            System.err.println("Erreur lors de l'ouverture du socket UDP : " + e.getMessage());
        }
    }

    private void listenForMessages() {
        byte[] buffer = new byte[1024];

        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                InetAddress senderAddress = packet.getAddress();

                String[] parts = message.split("\\|");

                if (parts.length < 3) continue;

                String command = parts[0];
                String senderId = parts[1];
                String senderNickname = parts[2];

                if (senderId.equals(this.myId)) {
                    continue;
                }

                if (command.equals("DISCOVER")) {
                    System.out.println("Nouveau venu détecté : " + senderNickname + " (" + senderAddress.getHostAddress() + ")");
                    listName.add(senderNickname);

                    sendDirectReply(senderAddress, packet.getPort());
                }
                else if (command.equals("REPLY")) {
                    System.out.println("Réponse reçue d'un utilisateur déjà présent : " + senderNickname);
                    if (!listName.contains(senderNickname)) {
                        listName.add(senderNickname);
                    }
                }

            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("Erreur de réception : " + e.getMessage());
                }
            }
        }
    }

    private void broadcastPresence() {
        try {
            String message = "DISCOVER|" + myId + "|" + myNickname;
            byte[] data = message.getBytes();

            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, DISCOVERY_PORT);

            socket.send(packet);
            System.out.println("Message de découverte envoyé en broadcast.");

        } catch (IOException e) {
            System.err.println("Erreur lors du broadcast : " + e.getMessage());
        }
    }

    private void sendDirectReply(InetAddress targetAddress, int targetPort) {
        try {
            String message = "REPLY|" + myId + "|" + myNickname;
            byte[] data = message.getBytes();

            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
            socket.send(packet);

        } catch (IOException e) {
            System.err.println("Erreur lors de la réponse : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String pseudo = "";

        while (pseudo.trim().isEmpty()) {
            System.out.print("Veuillez entrer votre pseudonyme : ");
            pseudo = scanner.nextLine();
        }

        System.out.println("Bonjour " + pseudo + " ! Connexion au réseau local...");

        DiscoveryService service = new DiscoveryService(pseudo);
        service.startService();

        while(true) {
            try {
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long usedMemoryMB = usedMemory / (1024 * 1024);

                System.out.println("Contacts : " + service.getListName() + " | RAM : " + usedMemoryMB + " Mo");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
