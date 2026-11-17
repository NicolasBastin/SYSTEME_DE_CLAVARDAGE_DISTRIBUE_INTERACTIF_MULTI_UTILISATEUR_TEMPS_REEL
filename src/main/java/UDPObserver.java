public interface UDPObserver {
    // Méthode appelée automatiquement quand le serveur UDP reçoit un message
    void onMessageReceived(UDPMessage message);
}