# Système de Clavardage Distribué - Réseau Local

Ce projet est une application de messagerie décentralisée en Java fonctionnant sur un réseau local. Il permet aux utilisateurs du même réseau de se découvrir automatiquement et d'échanger des messages sans serveur central.

Ce dépôt contient actuellement l'implémentation de la **Phase 1 : Découverte de contacts (Contact Discovery)**.

## Fonctionnalités actuelles (Phase 1)

*   **Découverte réseau (UDP Broadcast) :** Lorsqu'un utilisateur se connecte, il annonce sa présence à tout le réseau local via l'adresse de diffusion (`255.255.255.255`) sur le port `8888`.
*   **Réponses ciblées (Unicast) :** Les utilisateurs déjà présents sur le réseau interceptent l'annonce et répondent directement au nouvel arrivant.
*   **Choix du pseudonyme interactif :** L'utilisateur est invité à saisir son pseudonyme dans la console au lancement du service.
*   **Support multi-instances local :** Grâce à l'option `SO_REUSEADDR`, il est possible de lancer plusieurs instances de l'application sur la même machine pour effectuer des tests en local.
*   **Filtrage autonome :** Le système détecte et ignore ses propres paquets de diffusion (grâce à un identifiant unique généré au démarrage) pour éviter de s'ajouter soi-même en contact.

## Prérequis

*   **Java Development Kit (JDK)** : Version 8 ou supérieure.
*   Un terminal ou un IDE (comme IntelliJ IDEA, Eclipse ou VS Code).

## Compilation et Exécution

Si vous utilisez un IDE comme IntelliJ, vous pouvez simplement lancer la classe `DiscoveryService` via le bouton "Run".

Si vous utilisez le terminal, placez-vous dans le dossier contenant le fichier source (`src`) et exécutez les commandes suivantes :

1. **Compiler le code :**
   ```bash
   javac DiscoveryService.java