import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service P2P hybride UDP / TCP sur réseau local.
 * 
 * UDP (Port 8298) : Sert à la découverte des pairs (DISCOVER, REPLY, GOODBYE, DENIED).
 * TCP (Port 8299) : Sert à l'envoi direct et fiable des messages privés.
 */
public class DiscoveryService {
    private static final int DISCOVERY_PORT = 8298;
    private static final int TCP_PORT = 8299; // Nouveau port pour les messages TCP
    
    private DatagramSocket udpSocket;
    private ServerSocket tcpServerSocket; // Serveur pour écouter les messages TCP entrants

    private String myId;
    private String myNickname;
    
    // Remplacement de la Liste par une Map pour associer le pseudo à l'adresse IP (indispensable pour TCP)
    private ConcurrentHashMap<String, InetAddress> contacts = new ConcurrentHashMap<>();
    private volatile boolean nameConflict = false; 
    
    public boolean hasNameConflict() {
        return nameConflict;
    }

    public Set<String> getContactNames() {
        return contacts.keySet(); // Renvoie juste les pseudos pour l'affichage
    }

    public DiscoveryService(String nickname) {
        this.myNickname = nickname;
        this.myId = "ID_" + System.currentTimeMillis();
    }

    public void startService() {
        try {
            // 1. Démarrage du système de découverte UDP
            udpSocket = new DatagramSocket(null);
            udpSocket.setReuseAddress(true);
            udpSocket.bind(new InetSocketAddress(DISCOVERY_PORT));
            udpSocket.setBroadcast(true);

            Thread udpListenerThread = new Thread(this::listenForUDPMessages);
            udpListenerThread.start();

            // 2. Démarrage du serveur d'écoute TCP pour les messages privés
            startTCPServer();

            System.out.println("Services démarrés. Découverte (UDP:" + DISCOVERY_PORT + "), Messages (TCP:" + TCP_PORT + ")");
            broadcastPresence();

            // Hook de déconnexion propre
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try (DatagramSocket tempSocket = new DatagramSocket()) {
                    tempSocket.setBroadcast(true);
                    String message = "GOODBYE|" + myId + "|" + myNickname;
                    byte[] data = message.getBytes();
                    InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                    DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, DISCOVERY_PORT);
                    tempSocket.send(packet);
                } catch (IOException e) {
                    System.err.println("Erreur de déconnexion : " + e.getMessage());
                }
            }));

        } catch (SocketException e) {
            System.err.println("Erreur lors de l'ouverture du socket : " + e.getMessage());
        }
    }

    // Serveur TCP pour recevoir les messages ---
    private void startTCPServer() {
        Thread tcpThread = new Thread(() -> {
            try {
                tcpServerSocket = new ServerSocket(TCP_PORT);
                while (!tcpServerSocket.isClosed()) {
                    // Attend qu'une autre personne se connecte
                    Socket clientSocket = tcpServerSocket.accept();
                    // Lit le message entrant
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        String messageEntrant;
                        // On lit toutes les lignes envoyées jusqu'à la fin du flux
                        while ((messageEntrant = in.readLine()) != null) {
                            System.out.println(messageEntrant);
                        }
                    }
                    clientSocket.close();
                }
            } catch (IOException e) {
                if (!tcpServerSocket.isClosed()) {
                    System.err.println("Erreur Serveur TCP : " + e.getMessage());
                }
            }
        });
        tcpThread.start();
    }

    // --- Client TCP pour envoyer un message ---
    public void envoyerMessagePrive(String destinataire, String texte) {
        InetAddress ipDestinataire = contacts.get(destinataire);
        
        if (ipDestinataire == null) {
            System.out.println("[Erreur] L'utilisateur '" + destinataire + "' n'est pas dans votre liste de contacts.");
            return;
        }

        try (Socket socket = new Socket(ipDestinataire, TCP_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            out.println("[" + this.myNickname + "] (privé) : " + texte);
            out.flush();
            System.out.println("-> Message envoyé à " + destinataire);
    } 
        catch (IOException e) {
            System.err.println("[Erreur] Impossible de joindre " + destinataire + " : " + e.getMessage());
        }
    }

    // --- Écoute UDP (Stocke maintenant les IP) ---
    private void listenForUDPMessages() {
        byte[] buffer = new byte[1024];

        while (!udpSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                InetAddress senderAddress = packet.getAddress();
                String[] parts = message.split("\\|");

                if (parts.length < 3) continue;

                String command = parts[0];
                String senderId = parts[1];
                String senderNickname = parts[2];

                if (senderId.equals(this.myId)) continue;

                if (command.equals("DISCOVER")) {
                    if (senderNickname.equals(this.myNickname) || contacts.containsKey(senderNickname)) {
                        sendDenied(senderAddress, packet.getPort());
                    } else {
                        System.out.println("Nouveau venu détecté : " + senderNickname);
                        contacts.put(senderNickname, senderAddress); // On mémorise son IP !
                        sendDirectReply(senderAddress, packet.getPort());
                    }
                }
                else if (command.equals("DENIED")) {
                    System.out.println("\n[!] Probleme découvert : Le pseudonyme '" + senderNickname + "' est déjà utilisé.");
                    contacts.clear();
                    this.nameConflict = true;
                    udpSocket.close();
                    if (tcpServerSocket != null) tcpServerSocket.close();
                }
                else if (command.equals("GOODBYE")) {
                    System.out.println("Déconnexion de : " + senderNickname);
                    contacts.remove(senderNickname); // On le retire de la Map
                }
                else if (command.equals("REPLY")) {
                    if (!contacts.containsKey(senderNickname)) {
                        System.out.println("Réponse reçue d'un utilisateur présent : " + senderNickname);
                        contacts.put(senderNickname, senderAddress); // On mémorise son IP !
                    }
                }

            } catch (IOException e) {
                if (!udpSocket.isClosed()) {
                    System.err.println("Erreur de réception UDP : " + e.getMessage());
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
            udpSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur lors du broadcast : " + e.getMessage());
        }
    }

    private void sendDirectReply(InetAddress targetAddress, int targetPort) {
        try {
            String message = "REPLY|" + myId + "|" + myNickname;
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
            udpSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur de réponse : " + e.getMessage());
        }
    }

    private void sendDenied(InetAddress targetAddress, int targetPort) {
        try {
            String message = "DENIED|" + myId + "|" + myNickname;
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
            udpSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur d'envoi DENIED : " + e.getMessage());
        }
    }

    // --- MAIN pour lancer les 2 services ---
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
    
        while (true) {
            String pseudo = "";
            while (pseudo.trim().isEmpty()) {
                System.out.print("Veuillez entrer votre pseudo : ");
                pseudo = scanner.nextLine();
            }

            DiscoveryService service = new DiscoveryService(pseudo);
            service.startService();

            try {
                Thread.sleep(1000); // Attente pour validation réseau
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (service.hasNameConflict()) {
                continue; // Recommence si le pseudo est pris
            }

            ConnectionService dbService = new ConnectionService(pseudo);
            dbService.initialiserBaseDeDonnees();

            System.out.println("\nConnexion réussie ! Vos contacts : " + service.getContactNames());
            System.out.println("Pour parler, tapez : /msg <pseudo> <votre_message>");
            System.out.println("Pour quitter, tapez : /quit\n");

            // Boucle de messagerie
            while (!service.hasNameConflict()) {
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("/quit")) {
                    System.exit(0);
                } 

                else if (input.equalsIgnoreCase("/historique")) {
                    System.out.println("\n--- HISTORIQUE DES MESSAGES ---");
                    String historique = dbService.afficherMessages(); 
                    System.out.println(historique);
                    System.out.println("-------------------------------\n");
                }

                else if (input.startsWith("/msg ")) {
                    // Découpe la commande en 3 : ["/msg", "Destinataire", "Le reste du message"]
                    String[] parts = input.split(" ", 3);
                    if (parts.length == 3) {
                        String destinataire = parts[1];
                        String texteMessage = parts[2];
                        service.envoyerMessagePrive(destinataire, texteMessage);
                    } else {
                        System.out.println("Format incorrect. Utilisez : /msg <pseudo> <message>");
                    }
                } 
                else if (!input.trim().isEmpty()) {
                    System.out.println("Commande inconnue. Contacts en ligne : " + service.getContactNames());
                }
            }
        }
    }
}