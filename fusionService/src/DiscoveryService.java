import java.net.*;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service de base / découverte P2P (Peer-to-Peer) sur réseau local via UDP.
 * Permet à tous les utilisateurs de l'app de se détecter mutuellement
 * de manière décentralisée (sans serveur) sur le port 8298.
 *
 * Mécanismes implémentés :
 * - Protocole de base : Envoi d'un DISCOVER (broadcast) à la connexion, 
 *   auquel les autres app répondent par un REPLY (unicast).
 * - Anti-doublon : Si un pair détecte une tentative de connexion avec son propre 
 *   pseudonyme, il renvoie un DENIED forçant le nouvel arrivant à changer de nom. 
 * - Déconnexion propre : Un Hook de fermeture envoie un 
 *   message GOODBYE pour que les pairs mettent à jour leurs listes instantanément.
 */

public class DiscoveryService {
    private static final int DISCOVERY_PORT = 8298;
    private DatagramSocket socket;

    private String myId;
    private String myNickname;
    private List<String> listName = new CopyOnWriteArrayList<>();
    private volatile boolean nameConflict = false; 
    
    public boolean hasNameConflict() {
        return nameConflict;
    }

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

            // Le ShutdownHook est maintenant correctement placé DANS la méthode
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try (DatagramSocket tempSocket = new DatagramSocket()) {
                    tempSocket.setBroadcast(true);
                    
                    String message = "GOODBYE|" + myId + "|" + myNickname;
                    byte[] data = message.getBytes();
                    
                    InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                    DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, DISCOVERY_PORT);
                    
                    tempSocket.send(packet);
                    System.out.println("Signal de déconnexion envoyé aux autres utilisateurs.");
                    
                } catch (IOException e) {
                    System.err.println("Erreur lors de l'envoi du message de déconnexion : " + e.getMessage());
                }
            }));

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
                    if (senderNickname.equals(this.myNickname) || listName.contains(senderNickname)) {
                        System.out.println("Un utilisateur tente de se connecter avec le même pseudonyme : " + senderNickname);
                        sendDenied(senderAddress, packet.getPort());
                    } else {
                        System.out.println("Nouveau venu détecté : " + senderNickname + " (" + senderAddress.getHostAddress() + ")");
                        listName.add(senderNickname);
                        sendDirectReply(senderAddress, packet.getPort());
                    }
                }
                else if (command.equals("DENIED")) {
                    System.out.println("\n[!] AVERTISSEMENT : Le pseudonyme '" + senderNickname + "' est déjà utilisé sur le réseau.");
                    listName.clear();
                    this.nameConflict = true;
                    socket.close();
                }
                else if (command.equals("GOODBYE")) {
                    System.out.println("Un utilisateur s'est déconnecté : " + senderNickname);
                    listName.remove(senderNickname);
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

    private void sendDenied(InetAddress targetAddress, int targetPort) {
        try {
            String message = "DENIED|" + myId + "|" + myNickname;
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur lors de la réponse : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
    
        while (true) {
            String pseudo = "";

            while (pseudo.trim().isEmpty()) {
                System.out.print("Veuillez entrer votre pseudo : ");
                pseudo = scanner.nextLine();
            }

            System.out.println("Bonjour " + pseudo + " ! Connexion au réseau local...");

            // Lancement du service de base de données
            ConnectionService dbService = new ConnectionService(pseudo);

            // Lancement du service de découverte
            DiscoveryService service = new DiscoveryService(pseudo);
            service.startService();

            while (!service.hasNameConflict()) {
                try {
                    Runtime runtime = Runtime.getRuntime();
                    long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

                    System.out.println("Contacts : " + service.getListName() + " | RAM : " + usedMemoryMB + " Mo");
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            System.out.println("=> Reconnexion nécessaire avec un nouveau nom.\n");
        }
    }
}