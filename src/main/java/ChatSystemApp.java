import java.net.*;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ChatSystemApp implements UDPObserver, TCPObserver {
    
    private static final int DISCOVERY_PORT = 8298;
    private static final int TCP_PORT = 8299;

    private String myId;
    private String myNickname;
    private ConcurrentHashMap<String, InetAddress> contacts = new ConcurrentHashMap<>();
    private volatile boolean nameConflict = false;
    
    // On utilise maintenant BDDService (feu ConnectionService) pour gérer la base de données
    private BDDService dbService;

    public ChatSystemApp(String nickname) {
        this.myNickname = nickname;
        this.myId = "ID_" + System.currentTimeMillis();
        // Instanciation de votre nouvelle classe de base de données
        this.dbService = new BDDService(nickname);
    }

    public boolean hasNameConflict() {
        return nameConflict;
    }

    public void start() {
        // Démarrage de l'écoute UDP via UDPServer
        UDPServer udpServer = new UDPServer(DISCOVERY_PORT);
        udpServer.addObserver(this); 
        new Thread(udpServer).start();

        // Démarrage de l'écoute TCP via TCPServer
        TCPServer tcpServer = new TCPServer(TCP_PORT);
        tcpServer.addObserver(this);
        new Thread(tcpServer).start();

        // Initialisation de la base de données
        dbService.initialiserBaseDeDonnees();

        // 4. Envoi du broadcast de découverte via UDPSender
        try {
            UDPSender.broadcast("DISCOVER|" + myId + "|" + myNickname, DISCOVERY_PORT);
        } catch (IOException e) {
            System.err.println("Erreur de broadcast : " + e.getMessage());
        }

        // Déco propre (envoie GOODBYE quand on quitte)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                UDPSender.broadcast("GOODBYE|" + myId + "|" + myNickname, DISCOVERY_PORT);
            } catch (IOException e) {
                System.err.println("Erreur de déconnexion : " + e.getMessage());
            }
        }));
    }

    // --- RECEPTION UDP (Géré via UDPObserver) ---
    @Override
    public void onMessageReceived(UDPMessage msg) {
        if (msg.senderId().equals(this.myId)) return; // On ignore nos propres messages

        try {
            switch (msg.command()) {
                case "DISCOVER":
                    if (msg.senderNickname().equals(this.myNickname)) {
                        UDPSender.sendUnicast("DENIED|" + myId + "|" + myNickname, msg.senderAddress(), msg.senderPort());
                    } else {
                        System.out.println("Nouveau venu détecté : " + msg.senderNickname());
                        contacts.put(msg.senderNickname(), msg.senderAddress());
                        UDPSender.sendUnicast("REPLY|" + myId + "|" + myNickname, msg.senderAddress(), msg.senderPort());
                    }
                    break;
                case "DENIED":
                    System.out.println("\n[!] AVERTISSEMENT : Le pseudonyme '" + msg.senderNickname() + "' est déjà utilisé.");
                    contacts.clear();
                    this.nameConflict = true;
                    break;
                case "REPLY":
                    if (!contacts.containsKey(msg.senderNickname())) {
                        System.out.println("Réponse reçue d'un utilisateur présent : " + msg.senderNickname());
                        contacts.put(msg.senderNickname(), msg.senderAddress());
                    }
                    break;
                case "GOODBYE":
                    System.out.println("Déconnexion de : " + msg.senderNickname());
                    contacts.remove(msg.senderNickname());
                    break;
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la réponse UDP : " + e.getMessage());
        }
    }

    // --- RECEPTION TCP (Géré via TCPObserver) ---
    @Override
    public void onPrivateMessageReceived(String senderIp, String message) {
        System.out.println(message);
        // A Rajouter : Sauvegarde dans BDDService
        dbService.sauvegarderMessages(senderIp, message);

    }

    // --- ENVOI TCP ---
    private void envoyerMessagePrive(String destinataire, String texte) {
        InetAddress ipDestinataire = contacts.get(destinataire);
        
        if (ipDestinataire == null) {
            System.out.println("[Erreur] L'utilisateur '" + destinataire + "' n'est pas en ligne.");
            return;
        }

        try {
            String messageFormatte = "[" + this.myNickname + "] (privé) : " + texte;
            // Utilisation de la classe TCPSender
            TCPSender.sendMessage(ipDestinataire, TCP_PORT, messageFormatte);
            System.out.println("-> Message envoyé à " + destinataire);
            
            // Sauvegarde dans BDDService
            dbService.sauvegarderMessages(destinataire, texte); 

        } catch (IOException e) {
            System.err.println("[Erreur] Impossible de joindre " + destinataire + " : " + e.getMessage());
        }
    }

    // --- INTERFACE UTILISATEUR (Console) ---
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
    
        while (true) {
            String pseudo = "";
            while (pseudo.trim().isEmpty()) {
                System.out.print("Veuillez entrer votre pseudo : ");
                pseudo = scanner.nextLine();
            }

            ChatSystemApp app = new ChatSystemApp(pseudo);
            app.start();

            try {
                Thread.sleep(1000); // Attente pour validation réseau UDP
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (app.hasNameConflict()) {
                continue; // Recommence si le pseudo est pris
            }

            System.out.println("\nConnexion réussie ! Vos contacts : " + app.contacts.keySet());
            System.out.println("Commandes : /msg <pseudo> <msg> | /historique | /Contacts | /quit\n");

            // Boucle de messagerie
            while (!app.hasNameConflict()) {
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("/quit")) {
                    System.exit(0);
                } 
                else if (input.equalsIgnoreCase("/historique")) {
                    System.out.println("\n--- HISTORIQUE ---");
                    // Appel à BDDService
                    System.out.println(app.dbService.afficherMessages());
                    System.out.println("------------------\n");
                }
                else if (input.equalsIgnoreCase("/Contacts")) {
                    System.out.println("\n--- CONTACTS EN LIGNE ---");
                    System.out.println(app.contacts.keySet());
                    System.out.println("-------------------------\n");
                }
                else if (input.startsWith("/msg ")) {
                    String[] parts = input.split(" ", 3);
                    if (parts.length == 3) {
                        app.envoyerMessagePrive(parts[1], parts[2]);
                    } else {
                        System.out.println("Format incorrect. Utilisez : /msg <pseudo> <message>");
                    }
                }
                else if (!input.trim().isEmpty()) {
                    System.out.println("Commande inconnue.");
                }
            }
        }
    }
}