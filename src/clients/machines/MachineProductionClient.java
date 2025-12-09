package clients.machines;

import utils.Anomalie;
import utils.EtatMachine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MachineProductionClient {

    // Attributs
    private String id;
    private String libelle;
    private String type;
    private EtatMachine etat;

    // Connexion vers la station d'assemblage
    private String adresseStation;
    private int portStation;

    // Variables pour la connexion persistante
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connecte;

    public MachineProductionClient(String id, String libelle, String type,
                                   String adresseStation, int portStation) {
        this.id = id;
        this.libelle = libelle;
        this.type = type;
        this.etat = EtatMachine.ARRETE;
        this.adresseStation = adresseStation;
        this.portStation = portStation;
        this.connecte = false;

        System.out.println("[" + id + "] Machine créée: " + libelle);
    }

    // Démarrer la machine
    public void demarrer() {
        this.etat = EtatMachine.MARCHE;
        System.out.println("[" + id + "] Machine démarrée");
    }

    // Arrêter la machine
    public void arreter() {
        this.etat = EtatMachine.ARRETE;
        System.out.println("[" + id + "] Machine arrêtée");
    }

    // Se connecter à la station (connexion persistante)
    public boolean connecter() {
        if (connecte) {
            System.out.println("[" + id + "] Déjà connecté à la station");
            return true;
        }

        try {
            System.out.println("[" + id + "] Connexion à la station...");
            socket = new Socket(adresseStation, portStation);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connecte = true;
            System.out.println("[" + id + "] Connecté à la station");
            return true;

        } catch (IOException e) {
            System.err.println("[" + id + "] ERREUR: Connexion impossible - " + e.getMessage());
            return false;
        }
    }

    // Déposer une pièce à la station d'assemblage
    public String deposerPiece(String piece) {
        // Vérifier que la machine est en marche
        if (this.etat != EtatMachine.MARCHE) {
            return "ERREUR:Machine arrêtée";
        }

        // Si pas connecté, se connecter
        if (!connecte) {
            if (!connecter()) {
                return "ERREUR:Connexion impossible";
            }
        }

        try {
            // Envoyer la requête: DEPOSER:PIECE:ID_MACHINE
            String message = "DEPOSER:" + piece + ":" + id;
            out.println(message);
            System.out.println("[" + id + "] Message envoyé: " + message);

            // Attendre la réponse
            String reponse = in.readLine();
            System.out.println("[" + id + "] Réponse reçue: " + reponse);
            return reponse;

        } catch (IOException e) {
            System.err.println("[" + id + "] ERREUR: Communication - " + e.getMessage());
            connecte = false;
            return "ERREUR:Communication perdue";
        }
    }

    // Déposer plusieurs pièces avec une seule connexion
    public void deposerPieces(String[] pieces) {
        if (!connecter()) {
            System.err.println("[" + id + "] Impossible de se connecter");
            return;
        }

        for (String piece : pieces) {
            String resultat = deposerPiece(piece);
            System.out.println("[" + id + "] Résultat pour " + piece + ": " + resultat);

            // Petite pause entre les dépôts
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Se déconnecter de la station
    public void deconnecter() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            connecte = false;
            System.out.println("[" + id + "] Déconnecté de la station");
        } catch (IOException e) {
            System.err.println("[" + id + "] Erreur fermeture: " + e.getMessage());
        }
    }

    // Détecter une panne
    public Anomalie detecterPanne(String description) {
        System.out.println("[" + id + "] PANNE DÉTECTÉE: " + description);
        this.etat = EtatMachine.EN_PANNE;

        // Créer l'anomalie
        Anomalie anomalie = new Anomalie(
                (int)(Math.random() * 1000),
                new java.util.Date(),
                id,
                description
        );

        return anomalie;
    }

    // Getters
    public String getId() { return id; }
    public String getLibelle() { return libelle; }
    public String getType() { return type; }
    public EtatMachine getEtat() { return etat; }

    // Méthode main pour tester
    public static void main(String[] args) {
        System.out.println("=== TEST CLIENT MACHINE DE PRODUCTION ===");

        // Créer une machine de production
        MachineProductionClient machine = new MachineProductionClient(
                "M1",
                "Machine à Vis",
                "VIS",
                "localhost",
                8080
        );

        // Démarrer la machine
        machine.demarrer();

        // Se connecter une fois au début
        if (!machine.connecter()) {
            System.out.println("Échec de la connexion. Fin du test.");
            return;
        }

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        boolean continuer = true;
        int compteurPiece = 1;

        try {
            while (continuer) {
                // Générer un ID de pièce
                String piece = "VIS_" + String.format("%03d", compteurPiece);
                System.out.println("\n--- Dépôt de pièce " + compteurPiece + " ---");

                // Déposer la pièce
                String resultat = machine.deposerPiece(piece);

                // Demander si l'utilisateur veut continuer
                System.out.print("\nVoulez-vous déposer une autre pièce? (O/N): ");
                String reponse = console.readLine().trim().toUpperCase();

                if (reponse.equals("N") || reponse.equals("NON") || reponse.equals("NO")) {
                    continuer = false;
                    System.out.println("Fin des dépôts.");
                } else {
                    compteurPiece++;
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur de lecture console: " + e.getMessage());
        } finally {
            // Se déconnecter à la fin
            machine.deconnecter();
        }

        System.out.println("\n=== Test terminé ===");
    }
}