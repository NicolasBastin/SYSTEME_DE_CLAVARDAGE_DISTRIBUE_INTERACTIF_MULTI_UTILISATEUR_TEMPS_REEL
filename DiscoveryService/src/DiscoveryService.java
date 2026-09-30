import java.net.*;
import java.io.IOException;
import java.util.Scanner;

public class DiscoveryService {
    private static final int DISCOVERY_PORT = 8888;
    private DatagramSocket socket;

    private String myId;
    private String myNickname;

    // Constructeur qui prend le pseudo choisi par l'utilisateur
    public DiscoveryService(String nickname) {
        this.myNickname = nickname;
        // Génération d'un ID unique basé sur l'heure de lancement
        this.myId = "ID_" + System.currentTimeMillis();
    }

    public void startService() {
        try {
            // Création du socket avec l'option SO_REUSEADDR pour tester en local
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(DISCOVERY_PORT));
            socket.setBroadcast(true);

            // 1. Démarrer l'écouteur dans un Thread séparé
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.start();

            System.out.println("Service de découverte démarré. Écoute sur le port " + DISCOVERY_PORT);

            // 2. Annoncer sa présence à tout le réseau local
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
                socket.receive(packet); // Point bloquant en attente d'un message

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                InetAddress senderAddress = packet.getAddress();

                String[] parts = message.split("\\|");

                // Sécurité basique pour éviter un plantage si un message malformé arrive
                if (parts.length < 3) continue;

                String command = parts[0];
                String senderId = parts[1];
                String senderNickname = parts[2];

                // Si le message vient de moi-même (on compare les ID), je l'ignore
                if (senderId.equals(this.myId)) {
                    continue;
                }

                if (command.equals("DISCOVER")) {
                    System.out.println("Nouveau venu détecté : " + senderNickname + " (" + senderAddress.getHostAddress() + ")");
                    // TODO: Ajouter à la liste de contacts ici

                    // Lui répondre directement pour lui dire "Je suis là aussi"
                    sendDirectReply(senderAddress, packet.getPort());
                }
                else if (command.equals("REPLY")) {
                    System.out.println("Réponse reçue d'un utilisateur déjà présent : " + senderNickname);
                    // TODO: Ajouter à la liste de contacts ici
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

            // L'adresse 255.255.255.255 est l'adresse de broadcast générique
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

            // Envoi uniquement à l'IP de la personne qui a fait le DISCOVER
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
            socket.send(packet);

        } catch (IOException e) {
            System.err.println("Erreur lors de la réponse : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String pseudo = "";

        // Boucle pour s'assurer que l'utilisateur rentre un pseudo non vide
        while (pseudo.trim().isEmpty()) {
            System.out.print("Veuillez entrer votre pseudonyme : ");
            pseudo = scanner.nextLine();
        }

        System.out.println("Bonjour " + pseudo + " ! Connexion au réseau local...");

        // On instancie le service avec le pseudo saisi
        DiscoveryService service = new DiscoveryService(pseudo);
        service.startService();

        // Garder le programme en vie dans le thread principal
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}