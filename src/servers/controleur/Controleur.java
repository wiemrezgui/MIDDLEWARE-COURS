package servers.controleur;

import utils.Anomalie;
import utils.EtatMachine;

import java.io.*;
import java.net.*;
import java.util.*;

// Contrôleur - Serveur Multi-Thread Socket
// Gère les machines de production (pannes, démarrage, arrêt)
public class Controleur extends Thread {

    // Attributs
    private String id;
    private String libelle;
    private String adresseIP;
    private int port;
    private ServerSocket serverSocket;
    private boolean enFonctionnement;

    // Liste des machines gérées (synchronisée pour multi-threading)
    private Map<String, EtatMachine> machinesEtats;
    private List<Anomalie> anomalies;

    //Constructeur
    public Controleur(String id, String libelle, int port) {
        this.id = id;
        this.libelle = libelle;
        this.port = port;
        this.enFonctionnement = true;
        this.machinesEtats = Collections.synchronizedMap(new HashMap<>());
        this.anomalies = Collections.synchronizedList(new ArrayList<>());
    }

    // void run
    @Override
    public void run() {
        try {
            // Création du ServerSocket
            serverSocket = new ServerSocket(port);
            System.out.println("[Contrôleur] Serveur démarré");
            System.out.println("[Contrôleur] En attente des machines...\n");

            // Boucle d'écoute des connexions
            while (enFonctionnement) {
                try {
                    // Accepter une connexion d'une machine
                    Socket clientSocket = serverSocket.accept();

                    System.out.println("[Contrôleur] Nouvelle connexion " );

                    // Créer un thread pour gérer cette machine
                    ThreadGestionMachine handler = new ThreadGestionMachine(
                            clientSocket,
                            this);
                    handler.start();

                } catch (SocketException e) {
                    if (!enFonctionnement) {
                        System.out.println("[Contrôleur] Serveur arrêté proprement");
                    } else {
                        System.err.println("[Contrôleur] ERREUR socket: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("[Contrôleur] ERREUR I/O: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("[Contrôleur] ERREUR démarrage serveur: " + e.getMessage());
            e.printStackTrace();
        } finally {
            arreterServeur();
        }
    }

    // Traiter une panne signalée par une machine
    public synchronized String traiterPanne(String idMachine, String description) {
        System.out.println("\n[Contrôleur]  PANNE SIGNALÉE ");
        System.out.println("[Contrôleur] Machine: " + idMachine);
        System.out.println("[Contrôleur] Description: " + description);

        // Créer et enregistrer l'anomalie
        Anomalie anomalie = new Anomalie(
                anomalies.size() + 1,
                new Date(),
                idMachine,
                description
        );
        anomalies.add(anomalie);

        // Changer l'état de la machine défaillante
        machinesEtats.put(idMachine, EtatMachine.EN_PANNE);
        System.out.println("[Contrôleur] Machine " + idMachine + " mise en état: EN_PANNE");

        // Trouver une machine de remplacement
        String machineRemplacement = trouverMachineRemplacement(idMachine);

        String reponse;
        if (machineRemplacement != null) {
            // Activer la machine de remplacement
            machinesEtats.put(machineRemplacement, EtatMachine.MARCHE);

            System.out.println("[Contrôleur]  Solution trouvée:");
            System.out.println("[Contrôleur]   - ARRÊTER: " + idMachine);
            System.out.println("[Contrôleur]   - DÉMARRER: " + machineRemplacement);

            reponse = "OK:ARRETER:" + idMachine + ":DEMARRER:" + machineRemplacement;
        } else {
            System.out.println("[Contrôleur]  Aucune machine de remplacement disponible");
            reponse = "OK:ARRETER:" + idMachine + ":AUCUN_REMPLACEMENT";
        }

        System.out.println("[Contrôleur] Réponse: " + reponse + "\n");
        return reponse;
    }

    // Demarrer machine
    public synchronized String demarrerMachine(String idMachine) {
        System.out.println("\n[Contrôleur] Demande DÉMARRAGE: " + idMachine);

        EtatMachine etatActuel = machinesEtats.get(idMachine);

        if (etatActuel == null || etatActuel == EtatMachine.ARRETE) {
            machinesEtats.put(idMachine, EtatMachine.MARCHE);
            System.out.println("[Contrôleur]  Machine " + idMachine + " démarrée");
            return "OK:Machine démarrée";
        } else if (etatActuel == EtatMachine.MARCHE) {
            System.out.println("[Contrôleur]  Machine " + idMachine + " déjà en MARCHE\n");
            return "INFO:Déjà en marche";
        } else {
            System.out.println("[Contrôleur]  Machine " + idMachine + " en panne, impossible de démarrer\n");
            return "ERREUR:Machine en panne";
        }
    }

    // Arrêter une machine
    public synchronized String arreterMachine(String idMachine) {
        System.out.println("\n[Contrôleur] Demande ARRÊT: " + idMachine);

        EtatMachine etatActuel = machinesEtats.get(idMachine);

        if (etatActuel == EtatMachine.MARCHE ) {
            machinesEtats.put(idMachine, EtatMachine.ARRETE);
            System.out.println("[Contrôleur] Machine " + idMachine + " arrêtée");
            return "OK:Machine arrêtée";
        } else {
            System.out.println("[Contrôleur] ℹ Machine " + idMachine + " déjà arrêtée ou en panne\n");
            return "INFO:Déjà arrêtée ou en panne";
        }
    }

    //  Trouver une machine de remplacement
    private String trouverMachineRemplacement(String idMachine) {
        System.out.println("[Contrôleur] Recherche d'une machine de remplacement pour " + idMachine);

        // Parcourir toutes les machines enregistrées dans le Map
        synchronized (machinesEtats) {
            for (Map.Entry<String, EtatMachine> entry : machinesEtats.entrySet()) {
                String machineId = entry.getKey();
                EtatMachine etat = entry.getValue();

                // Ne pas considérer la machine en panne elle-même
                if (!machineId.equals(idMachine)) {
                    // Si la machine est arrêtée, on peut la démarrer comme remplacement
                    if (etat == EtatMachine.ARRETE) {
                        System.out.println("[Contrôleur] Machine de remplacement trouvée: " + machineId);

                        // Démarrer la machine de remplacement
                        String resultatDemarrage = demarrerMachine(machineId);
                        System.out.println("[Contrôleur] Résultat du démarrage de " + machineId + ": " + resultatDemarrage);

                        // Retourner l'ID de la machine démarrée
                        return machineId;
                    }
                }
            }
        }

        System.out.println("[Contrôleur] Aucune machine de remplacement disponible");
        return null;
    }


    public void arreterServeur() {
        try {
            enFonctionnement = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("\n[Contrôleur] Serveur fermé");
            }
        } catch (IOException e) {
            System.err.println("[Contrôleur] Erreur fermeture serveur: " + e.getMessage());
        }
    }

    // Getters
    public Map<String, EtatMachine> getMachinesEtats() { return machinesEtats; }
    public List<Anomalie> getAnomalies() { return anomalies; }

    // Main pour lancer le contrôleur
    public static void main(String[] args) {
        // Créer et démarrer le contrôleur sur le port 9090
        Controleur controleur = new Controleur("Controleur 1 ", "Contrôleur Principal", 9090);
        controleur.start();

        System.out.println("Le contrôleur est en écoute...");
    }
}

