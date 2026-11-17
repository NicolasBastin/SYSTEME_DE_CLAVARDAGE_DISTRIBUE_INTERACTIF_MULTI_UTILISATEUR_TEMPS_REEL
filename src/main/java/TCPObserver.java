public interface TCPObserver {
    // Méthode appelée automatiquement quand le serveur TCP reçoit un message privé
    void onPrivateMessageReceived(String senderIp, String message);
}