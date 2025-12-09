package servers.StationAssemblage;

import utils.ZoneStockage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class StationAssemblageServer implements IStationAssemblage{
        // Attributs
        private int id;
        private String libelle;
        private String adresseIP;
        private int port;
        private ServerSocket serverSocket;

        // Zones de stockage pour différentes pièces
        private ZoneStockage[] zones;
        public StationAssemblageServer(int id, String libelle, int port, int nbZones) {
            this.id = id;
            this.libelle = libelle;
            this.port = port;

            // Initialisation des zones de stockage
            this.zones = new ZoneStockage[nbZones];
            for (int i = 0; i < nbZones; i++) {
                zones[i] = new ZoneStockage("Zone_" + (i+1), "Libelle zone " + (i+1), 10, 0);
            }

            System.out.println("[Station] " + libelle + " initialisée sur le port " + port);
        }

        //Démarrage du serveur Socket mono-thread
        public void demarrerServeur() {
            try {
                // Création du ServerSocket
                serverSocket = new ServerSocket(port);
                System.out.println("[Station] Serveur démarré sur port " + port);
                System.out.println("[Station] En attente des machines...");

                // Boucle d'écoute des connexions
                while (true) {
                    try {
                        // Accepter une connexion client (machine)
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("[Station] Connexion reçue de: " +
                                clientSocket.getInetAddress());

                        // Traiter la requête directement
                        traiterRequeteMachine(clientSocket);

                    } catch (SocketException e) {
                        System.err.println("[Station] Erreur : " + e.getMessage());
                    } catch (IOException e) {
                        System.err.println("[Station] Erreur I/O: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                System.err.println("[Station] Erreur démarrage serveur: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Traiter une requête d'une machine
        private void traiterRequeteMachine(Socket clientSocket) {
            BufferedReader in = null;
            PrintWriter out = null;

            try {
                // Création des flux d'entrée/sortie
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                System.out.println("[Station] Prêt à recevoir des messages de " +
                        clientSocket.getInetAddress());

                // Boucle pour recevoir plusieurs messages sur la même connexion
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("[Station] Message reçu: " + message);

                    if (!message.isEmpty()) {
                        // Parser le message: format "DEPOSER:PIECE:ID_MACHINE"
                        String[] parts = message.split(":");

                        if (parts.length >= 2) {
                            String action = parts[0];
                            String piece = parts[1];
                            String reponse = stockerPiece(piece);

                            // Envoyer la réponse
                            out.println(reponse);
                            System.out.println("[Station] Pour l'action "+ action +
                                    " \nRéponse envoyée :  " + reponse);
                        } else {
                            out.println("ERREUR:Format message invalide");
                        }
                    }
                }

            } catch (IOException e) {
                System.err.println("[Station] Erreur traitement requête: " + e.getMessage());
            } finally {
                // Fermer les ressources seulement quand la connexion est terminée
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (clientSocket != null) clientSocket.close();
                    System.out.println("[Station] Connexion fermée avec " +
                            (clientSocket != null ? clientSocket.getInetAddress() : "client"));
                } catch (IOException e) {
                    System.err.println("[Station] Erreur fermeture: " + e.getMessage());
                }
            }
        }

        // Stocker une pièce dans une zone
        @Override
        public String stockerPiece(String piece) {
            try {
                // Parcourir les zones jusqu'à trouver une avec de la place
                for (ZoneStockage zone : zones) {
                    if (!zone.estPleine()) {
                        // Stocker la pièce dans cette zone
                        zone.setQuantiteActuelle(zone.getQuantiteActuelle() + 1);
                        return "Pièce stockée dans zone " + zone.getId();
                    }
                }

                return "ERREUR: Toutes les zones sont pleines";

            } catch (Exception e) {
                return "ERREUR: " + e.getMessage();
            }
        }

        // Getters
        public int getId() { return id; }
        public String getLibelle() { return libelle; }
        public int getPort() { return port; }

    public static void main(String[] args) {
        System.out.println("=== TEST SERVEUR STATION D'ASSEMBLAGE ===");

        // Créer une station sur le port 8080 avec 3 zones
        StationAssemblageServer station = new StationAssemblageServer(1, "Station Centrale", 8080, 3);

        // Démarrer le serveur dans un thread séparé
        Thread serveurThread = new Thread(() -> {
            station.demarrerServeur();
        });
        serveurThread.start();

        System.out.println("Serveur lancé sur localhost:8080");
        System.out.println("En attente des connexions des machines...");

        // Attendre un peu pour voir les messages
        try {
            Thread.sleep(30000); // Attendre 30 secondes
            System.out.println("Test terminé.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}