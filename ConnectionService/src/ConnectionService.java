import java.net.*;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.net.InetAddress;
import DiscoveryService.java;

/**
 * Service de base / découverte P2P (Peer-to-Peer) sur réseau local via UDP.
 * Permet à tous les utilisateurs de l'app de se détecter mutuellement
 * de manière décentralisée (sans serveur) sur le port 8298.
 *
 * Mécanismes implémentés :
 * - Protocole de base : Envoi d'un DISCOVER (broadcast) à la connexion, 
 *   auquel les autres app répondent par un REPLY (unicast).
 * - Contre Collision : Délai aléatoire (50-300 ms) avant de répondre à un 
 *   DISCOVER pour éviter les collisions de paquets sur le réseau.
 * - Detection de déconnexion sauvage : Envoi de signaux ALIVE toutes les 10 secondes. 
 *   Tout contact inactif depuis plus de 30 secondes est supprimé de la liste.
 * - Déconnexion propre : Un Hook de fermeture envoie un 
 *   message GOODBYE pour que les pairs mettent à jour leurs listes instantanément.
 * - Anti-doublon : Si un pair détecte une tentative de connexion avec son propre 
 *   pseudonyme, il renvoie un DENIED forçant le nouvel arrivant à changer de nom.
 */

public class ConnectionService {

    //On pointe vers le fichier qui contient les messages
    File fichierBDD = new File("application.db");

    // On crée un seul Scanner pour toutes les saisies
    Scanner sc = new Scanner(System.in);
    System.out.println("Rentrez un pseudo :");
    String pseudo = sc.nextLine();

    //### VERIFICATION DE L'UNICITE DU PSEUDO A FAIRE ###

    // Connexion au compte si la database message existe
    if (fichierBDD.exists()) {
        System.out.println("La base de données existe déjà");


    }

    // Connexion au compte si la database message n'existe pas
    else{
        // Requête SQL pour créer une table message
        String sql = "CREATE TABLE IF NOT EXISTS utilisateurs (" +
                " horodatage DATE," +
                " messages TEXT," +
                " adresseIP TEXT UNIQUE" +
                ");";

    }
}
