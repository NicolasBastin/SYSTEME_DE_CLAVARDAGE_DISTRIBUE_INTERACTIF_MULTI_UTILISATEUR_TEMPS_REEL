import java.net.*;
import java.io.IOException;

public class UDPSender {
    // Méthode statique pour le Broadcast (utilisé pour DISCOVER)
    public static void broadcast(String message, int port) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] data = message.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, port);
            socket.send(packet);
        }
    }

    // Méthode statique pour l'Unicast (utilisé pour REPLY, DENIED)
    public static void sendUnicast(String message, InetAddress targetIp, int targetPort) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, targetIp, targetPort);
            socket.send(packet);
        }
    }
}