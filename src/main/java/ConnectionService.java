import java.io.File;
import java.sql.*;

public class ConnectionService {

    // on cree un pointeur vers une base de données
    private File fichierBDD = new File("application.db");

    // Définition de l'URL pour la base de données
    private String url = "jdbc:sqlite:" + fichierBDD.getName();
    private String pseudo;

    public ConnectionService(String pseudo) {
        this.pseudo = pseudo;
    }

    public void initialiserBaseDeDonnees() {
        if (fichierBDD.exists()) {
            System.out.println("La base de données existe déjà pour : " + pseudo);
        } else {
            System.out.println("Création de la base de données...");
        }

        String sql = "CREATE TABLE IF NOT EXISTS historique (" +
                " horodatage TEXT," +
                " messages TEXT," +
                " adresseIPdestinataire TEXT UNIQUE" +
                ");";

        // Exécution et création de la table
        try (Connection conn = DriverManager.getConnection(url); // creation de la connexion à la bdd à l'aide de l'url avec le bon driver
             Statement stmt = conn.createStatement()) { //creation d'un objet statement pour executer des commandes sql
            stmt.execute(sql);
            System.out.println("Table 'historique' initialisée avec succès.");
        }
        catch (SQLException e) {
            System.out.println("Erreur lors de l'initialisation : " + e.getMessage());
        }
    }

    public void sauvegarderMessages(String expediteur, String texte) {
        String sql = "INSERT INTO historique (horodatage, messages, adresseDestinataire) VALUES (datetime('now', 'localtime'), ?, ?)";

        // Ouverture de la connexion et préparation de la requête
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Remplacement des '?' par nos variables
            pstmt.setString(1, texte);
            pstmt.setString(2, expediteur);

            // Exécution et mise à jour
            pstmt.executeUpdate();
            System.out.println("Message sauvegardé avec succès dans l'historique.");

        } catch (SQLException e) {
            System.out.println("Erreur lors de la sauvegarde du message");
        }
    }

    //A TESTER
    String afficherMessages(String expediteur) {
        String allMessages = "";
        String sql = "SELECT messages FROM historique (horodatage, messages, adresseDestinataire)";

        // Ouverture de la connexion et préparation de la requête
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Parcours des résultats
            while (rs.next()) {
                allMessages = rs.getString("messages");
            }
        }
        catch (SQLException e) {
            System.out.println("Erreur lors de la lecture des messages");
        }

        return allMessages;
    }

}
