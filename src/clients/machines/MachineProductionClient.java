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

    private String id;
    private String libelle;
    private String type;
    private EtatMachine etat;

    // Connexion vers la station d'assemblage
    private String adresseStation;
    private int portStation;
    private Socket socketStation;
    private BufferedReader inStation;
    private PrintWriter outStation;
    private boolean connecteStation;

    // Connexion vers le contrôleur
    private String adresseControleur;
    private int portControleur;

    // CONSTRUCTEUR
    public MachineProductionClient(String id, String libelle, String type,
                                   String adresseStation, int portStation,
                                   String adresseControleur, int portControleur) {
        this.id = id;
        this.libelle = libelle;
        this.type = type;
        this.etat = EtatMachine.ARRETE;
        this.adresseStation = adresseStation;
        this.portStation = portStation;
        this.adresseControleur = adresseControleur;
        this.portControleur = portControleur;
        this.connecteStation = false;

        System.out.println("[" + id + "] Machine créée: " + libelle);
        System.out.println("[" + id + "] Station: " + adresseStation + ":" + portStation);
        System.out.println("[" + id + "] Contrôleur: " + adresseControleur + ":" + portControleur);
    }

    // Envoyer un message au contrôleur
    private String envoyerAuControleur(String message) {
        Socket socketCtrl = null;
        BufferedReader inCtrl = null;
        PrintWriter outCtrl = null;

        try {
            System.out.println("\n[" + id + "] Connexion au contrôleur...");

            // Établir la connexion
            socketCtrl = new Socket(adresseControleur, portControleur);
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));

            System.out.println("[" + id + "] Connecté au contrôleur");

            // Envoyer le message
            outCtrl.println(message);
            System.out.println("[" + id + "] Message envoyé: " + message);

            // Attendre la réponse
            String reponse = inCtrl.readLine();
            System.out.println("[" + id + "] Réponse reçue: " + reponse);

            return reponse;

        } catch (UnknownHostException e) {
            System.err.println("[" + id + "] ERREUR: Hôte contrôleur inconnu - " + e.getMessage());
            return "ERREUR:Hôte inconnu";

        } catch (IOException e) {
            System.err.println("[" + id + "] ERREUR: Communication contrôleur - " + e.getMessage());
            return "ERREUR:Communication impossible";

        } finally {
            // Fermer la connexion
            try {
                if (inCtrl != null) inCtrl.close();
                if (outCtrl != null) outCtrl.close();
                if (socketCtrl != null) socketCtrl.close();
                System.out.println("[" + id + "] Connexion contrôleur fermée\n");
            } catch (IOException e) {
                System.err.println("[" + id + "] Erreur fermeture contrôleur: " + e.getMessage());
            }
        }
    }

    // Demander le démarrage au contrôleur
    public String demanderDemarrage() {
        System.out.println(" DEMANDE DE DÉMARRAGE           ");

        // Envoyer: DEMARRER:ID_MACHINE
        String message = "DEMARRER:" + id;
        String reponse = envoyerAuControleur(message);

        // Traiter la réponse
        if (reponse != null && reponse.startsWith("OK")) {
            this.etat = EtatMachine.MARCHE;
            System.out.println("[" + id + "] Machine démarrée (état: MARCHE)");
        } else {
            System.err.println("[" + id + "] Démarrage refusé: " + reponse);
        }

        return reponse;
    }

    // Signaler une panne au contrôleur

    public String signalerPanne(String description) {
        System.out.println("PANNE DÉTECTÉE         ");
        System.out.println("[" + id + "] Description: " + description);

        // Changer l'état de la machine
        this.etat = EtatMachine.EN_PANNE;

        // Déconnecter de la station si nécessaire
        if (connecteStation) {
            deconnecterStation();
        }

        // Envoyer au contrôleur: PANNE:ID_MACHINE:DESCRIPTION
        String message = "PANNE:" + id + ":" + description;
        String reponse = envoyerAuControleur(message);

        // Traiter la réponse
        if (reponse != null && reponse.startsWith("OK")) {
            System.out.println("[" + id + "] Panne traitée par le contrôleur");
            System.out.println("[" + id + "] Actions: " + reponse);

            // Parser la réponse pour voir quelle machine de remplacement
            if (reponse.contains("DEMARRER")) {
                String[] parts = reponse.split(":");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("DEMARRER") && i + 1 < parts.length) {
                        System.out.println("[" + id + "] Machine de remplacement: " + parts[i + 1]);
                    }
                }
            }
        } else {
            System.err.println("[" + id + "] ✗ Erreur traitement panne: " + reponse);
        }

        return reponse;
    }
    // ÉTAPE 2: Se connecter à la station
    public boolean connecterStation() {
        if (connecteStation) {
            System.out.println("[" + id + "] Déjà connecté à la station");
            return true;
        }

        try {
            System.out.println("\n[" + id + "] Connexion à la station...");
            socketStation = new Socket(adresseStation, portStation);
            outStation = new PrintWriter(socketStation.getOutputStream(), true);
            inStation = new BufferedReader(new InputStreamReader(socketStation.getInputStream()));
            connecteStation = true;
            System.out.println("[" + id + "] Connecté à la station\n");
            return true;

        } catch (IOException e) {
            System.err.println("[" + id + "] ERREUR: Connexion station impossible - " + e.getMessage());
            return false;
        }
    }

    // Déposer une pièce à la station
    public String deposerPiece(String piece) {
        // Vérifier que la machine est en marche
        if (this.etat != EtatMachine.MARCHE) {
            System.err.println("[" + id + "] ✗ Machine arrêtée, impossible de produire");
            return "ERREUR:Machine arrêtée";
        }

        // Si pas connecté à la station, se connecter
        if (!connecteStation) {
            if (!connecterStation()) {
                return "ERREUR:Connexion station impossible";
            }
        }

        try {
            // Envoyer: DEPOSER:PIECE:ID_MACHINE
            String message = "DEPOSER:" + piece + ":" + id;
            outStation.println(message);
            System.out.println("[" + id + "] Envoyé à station: " + message);

            // Attendre la réponse
            String reponse = inStation.readLine();
            System.out.println("[" + id + "] Réponse station: " + reponse);
            return reponse;

        } catch (IOException e) {
            System.err.println("[" + id + "] ERREUR: Communication station - " + e.getMessage());
            connecteStation = false;
            return "ERREUR:Communication perdue";
        }
    }

    // Se déconnecter de la station
    public void deconnecterStation() {
        try {
            if (inStation != null) inStation.close();
            if (outStation != null) outStation.close();
            if (socketStation != null) socketStation.close();
            connecteStation = false;
            System.out.println("[" + id + "] Déconnecté de la station");
        } catch (IOException e) {
            System.err.println("[" + id + "] Erreur fermeture station: " + e.getMessage());
        }
    }


    // Produire des pièces
    public void produire(String typePiece, int quantite) {
        System.out.println(" DÉBUT PRODUCTION");
        System.out.println("[" + id + "] Type: " + typePiece);
        System.out.println("[" + id + "] Quantité: " + quantite + "\n");

        for (int i = 0; i < quantite; i++) {
            if (this.etat == EtatMachine.MARCHE) {
                try {
                    // Simuler le temps de fabrication
                    System.out.println("[" + id + "]  Fabrication pièce " + (i + 1) + "...");
                    Thread.sleep(2000);

                    // Créer l'identifiant de la pièce
                    String piece = typePiece + "_" + String.format("%03d", i + 1);
                    System.out.println("[" + id + "] Pièce fabriquée: " + piece);

                    // Déposer la pièce à la station
                    String resultat = deposerPiece(piece);

                    // Vérifier le résultat
                    if (resultat.startsWith("ERREUR")) {
                        System.err.println("[" + id + "]  Échec dépôt, arrêt production");
                        break;
                    }

                    System.out.println(); // Ligne vide pour lisibilité

                } catch (InterruptedException e) {
                    System.err.println("[" + id + "] Production interrompue");
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                System.out.println("[" + id + "] Production arrêtée (état: " + etat + ")");
                break;
            }
        }

        System.out.println("\n[" + id + "] ═══ FIN PRODUCTION ═══\n");
    }


    // GETTERS

    public String getId() { return id; }
    public String getLibelle() { return libelle; }
    public String getType() { return type; }
    public EtatMachine getEtat() { return etat; }


    public static void main(String[] args) {
        System.out.println("TEST MACHINE DE PRODUCTION COMPL");
        System.out.println("Scénario: Démarrage → Production → Panne         ");

        // Créer la machine
        MachineProductionClient machine = new MachineProductionClient(
                "M1",
                "Machine Principale",
                "VIS",
                "localhost", 8080,  // Station d'assemblage
                "localhost", 9090   // Contrôleur
        );

        try {
            // ÉTAPE 1: Demander le démarrage au contrôleur
            System.out.println("\n━━━ ÉTAPE 1: DÉMARRAGE ━━━");
            machine.demanderDemarrage();
            Thread.sleep(2000);

            // ÉTAPE 2: Produire des pièces
            System.out.println("\n━━━ ÉTAPE 2: PRODUCTION ━━━");
            machine.produire("VIS", 3);
            Thread.sleep(2000);

            // ÉTAPE 3: Simuler une panne
            System.out.println("\n━━━ ÉTAPE 3: PANNE ━━━");
            machine.signalerPanne("Surchauffe moteur - température 85°C");
            Thread.sleep(2000);

            System.out.println(" TEST TERMINÉ AVEC SUCCÈS     ");

        } catch (InterruptedException e) {
            System.err.println("Test interrompu: " + e.getMessage());
        }
    }
}