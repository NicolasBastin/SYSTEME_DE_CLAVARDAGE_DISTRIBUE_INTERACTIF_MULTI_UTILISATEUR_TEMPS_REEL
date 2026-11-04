import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class ConnectionService {

    private File fichierBDD = new File("application.db");
    private String pseudo;

    public ConnectionService(String pseudo) {
        this.pseudo = pseudo;
        initialiserBaseDeDonnees();
    }

    private void initialiserBaseDeDonnees() {
        if (fichierBDD.exists()) {
            System.out.println("La base de données existe déjà pour : " + pseudo);
        } else {
            System.out.println("Création de la base de données...");
            String sql = "CREATE TABLE IF NOT EXISTS utilisateurs (" +
                    " horodatage DATE," +
                    " messages TEXT," +
                    " adresseIP TEXT UNIQUE" +
                    ");";
        }
    }
}