import java.net.InetAddress;

// Je sais plus si on l'a vu mais le record permet de créer une classe avec des champs immuables et un constructeur automatiquement généré. 
// C'est pratique pour les messages UDP car on ne veut pas que ces informations changent après la création du message.
public record UDPMessage(
    String command, 
    String senderId, 
    String senderNickname, 
    InetAddress senderAddress, 
    int senderPort
) {}
