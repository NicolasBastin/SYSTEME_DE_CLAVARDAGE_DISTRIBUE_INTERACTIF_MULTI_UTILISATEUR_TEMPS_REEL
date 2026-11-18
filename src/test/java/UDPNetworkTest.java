import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class UDPNetworkTest {

    // Variable simple pour stocker le message reçu
    private UDPMessage messageRecu = null;
    private UDPMessage messageRecu2 = null;

    @Test
    void testCreationMessage() throws Exception {
        InetAddress ip = InetAddress.getByName("127.0.0.1");
        UDPMessage msg = new UDPMessage("DISCOVER", "id1", "Nicolas", ip, 5000);

        assertEquals("DISCOVER", msg.command());
        assertEquals("Nicolas", msg.senderNickname());
    }

    @Test
    void testEnvoiEtReceptionUDP() throws Exception {
        int port = 12345;
        UDPServer server = new UDPServer(port);

        // Dès que le serveur reçoit un message, on le sauvegarde
        server.addObserver(msg -> messageRecu = msg);

        // On démarre le serveur en arrière-plan
        Thread thread = new Thread(server);
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(100); // Pause pour laisser le serveur démarrer

        // On envoie un message du thread vers notre ordi
        UDPSender.sendUnicast("REPLY|id2|Alice", InetAddress.getByName("127.0.0.1"), port);

        Thread.sleep(100); // Petite pause pour laisser le message arriver

        // Vérification
        assertNotNull(messageRecu);
        assertEquals("REPLY", messageRecu.command());
        assertEquals("Alice", messageRecu.senderNickname());
    }

    @Test
    void testEnvoiPaquetIncompletIgnoree() throws Exception {
        int port = 12346;
        UDPServer server = new UDPServer(port);

        // Dès que le serveur reçoit un message, on le sauvegarde
        server.addObserver(msg -> messageRecu2 = msg);

        // On démarre le serveur en arrière-plan
        Thread thread = new Thread(server);
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(100); // Pause pour laisser le serveur démarrer

        // On envoie un message du thread vers notre ordi
        UDPSender.sendUnicast("REPLY|id_sans_pseudo", InetAddress.getByName("127.0.0.1"), port);

        Thread.sleep(100); // Petite pause pour laisser le message arriver

        // Vérification que le paquet incomplet est ignoree
        assertNull(messageRecu2);
    }

}