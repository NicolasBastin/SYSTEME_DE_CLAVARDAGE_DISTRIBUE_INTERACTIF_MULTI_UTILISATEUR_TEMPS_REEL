import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


//### DEMANDER AU PROF SI ON PEUT DEMANDER AUX UTILISATEURS D'INSTALLER DES DRIVERS SQLITE###

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
}
