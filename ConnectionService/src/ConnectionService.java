import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class ConnectionService {

    // On pointe vers le fichier qui contient les messages
    File fichierBDD = new File("application.db");
    // URL JDBC indispensable pour créer/ouvrir le fichier SQLite
    String url = "jdbc:sqlite:" + fichierBDD.getName();

    // On crée un seul Scanner pour toutes les saisies
    Scanner sc = new Scanner(System.in);
    private boolean pseudoAlreadyTaken = true;

    // TOUT LE CODE DOIT ÊTRE DANS UNE MÉTHODE
    public void initialiser() {
        while (pseudoAlreadyTaken){
            System.out.println("Rentrez un pseudo :");
            String pseudo = sc.nextLine();
            if (pseudo != null && !pseudo.isEmpty()){ // Correction du if qui était vide
                //### VERIFICATION DE L'UNICITE DU PSEUDO A FAIRE ###
                pseudoAlreadyTaken = false;
            }
        }

        // Connexion au compte si la database message existe
        if (fichierBDD.exists()) {
            System.out.println("La base de données des messages existe déjà, on passe à la connexion");
        }
        // Connexion au compte si la database message n'existe pas
        else {
            System.out.println("La base de données des messages a été crée, on passe à la connexion");
        }

        // Requête SQL déplacée ici pour être visible par le try-catch
        String sql = "CREATE TABLE IF NOT EXISTS utilisateurs (" +
                " horodatage DATE," +
                " messages TEXT," +
                " adresseIPdestinataire TEXT UNIQUE" +
                ");";

        // RAJOUT DU TRY-CATCH POUR TOUT EXÉCUTER
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("Table 'utilisateurs' créée ou déjà existante avec succès.");

        } catch (SQLException e) {
            System.out.println("Erreur SQL lors de l'initialisation : " + e.getMessage());
        }
    }

    // AJOUT DU MAIN POUR TESTER LA CRÉATION
    public static void main(String[] args) {
        ConnectionService service = new ConnectionService();
        service.initialiser();
    }
}

