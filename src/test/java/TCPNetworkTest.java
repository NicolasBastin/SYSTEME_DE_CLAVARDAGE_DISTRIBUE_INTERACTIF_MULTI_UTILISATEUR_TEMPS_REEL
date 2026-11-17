import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import static org.junit.jupiter.api.Assertions.*;

class TCPNetworkTest {

    private String texteRecu = null;

    @Test
    void testEnvoiEtReceptionTCP() throws Exception {
        int port = 23456;
        TCPServer server = new TCPServer(port);

        // Dès que le serveur reçoit un message, on le sauvegarde
        server.addObserver((ip, message) -> texteRecu = message);

        // On démarre le serveur en arrière-plan
        Thread thread = new Thread(server);
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(100);

        // On envoie un message privé
        TCPSender.sendMessage(InetAddress.getByName("127.0.0.1"), port, "Salut !");

        Thread.sleep(100);

        // Vérification
        assertEquals("Salut !", texteRecu);
    }
}