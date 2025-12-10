package servers.controleur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Thread pour gérer la communication avec une machine
class ThreadGestionMachine extends Thread {

    private Socket socket;
    private Controleur controleur;

    public ThreadGestionMachine(Socket socket, Controleur controleur) {
        this.socket = socket;
        this.controleur = controleur;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            System.out.println("[Thread Démarré");

            // Créer les flux d'entrée/sortie
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Lire le message de la machine
            String message = in.readLine();

            if (message == null) {
                System.out.println("[Thread- Message vide reçu");
                return;
            }

            System.out.println("[Thread- Message reçu: " + message);

            // Parser le message: ACTION:ID_MACHINE:PARAMS
            String[] parts = message.split(":", 3);
            String reponse = "";

            if (parts.length >= 2) {
                String action = parts[0];
                String idMachine = parts[1];

                // Traiter selon l'action demandée
                switch (action) {
                    case "PANNE":
                        // PANNE:ID_MACHINE:DESCRIPTION
                        String description = parts.length > 2 ? parts[2] : "Panne non spécifiée";
                        reponse = controleur.traiterPanne(idMachine, description);
                        break;

                    case "DEMARRER":
                        // DEMARRER:ID_MACHINE
                        reponse = controleur.demarrerMachine(idMachine);
                        break;

                    case "ARRETER":
                        // ARRETER:ID_MACHINE
                        reponse = controleur.arreterMachine(idMachine);
                        break;
                    default:
                        reponse = "ERREUR:Action inconnue '" + action + "'";
                        System.err.println("[Thread- Action inconnue: " + action);
                }
            } else {
                reponse = "ERREUR:Format message invalide (attendu: ACTION:ID:PARAMS)";
                System.err.println("[Thread- Format invalide: " + message);
            }

            // Envoyer la réponse à la machine
            out.println(reponse);
            System.out.println("[Thread- Réponse envoyée: " + reponse);

        } catch (IOException e) {
            System.err.println("[Thread-  ERREUR I/O: " + e.getMessage());
        } finally {
            // Fermer les ressources
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
                System.out.println("[Thread- Connexion fermée\n");
            } catch (IOException e) {
                System.err.println("[Thread- Erreur fermeture: " + e.getMessage());
            }
        }
    }
}
