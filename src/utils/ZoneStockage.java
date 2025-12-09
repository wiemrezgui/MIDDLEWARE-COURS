package utils;

public class ZoneStockage {
    private String id;
    private String libelle;
    private int capaciteMax;
    private int quantiteActuelle;

    public ZoneStockage(String id, String libelle, int capaciteMax, int quantiteActuelle) {
        this.id = id;
        this.libelle = libelle;
        this.capaciteMax = capaciteMax;
        this.quantiteActuelle = quantiteActuelle;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public int getQuantiteActuelle() {
        return quantiteActuelle;
    }

    public void setQuantiteActuelle(int quantiteActuelle) {
        this.quantiteActuelle = quantiteActuelle;
    }

    // Vérifier si la zone est pleine
    public boolean estPleine() {
        return quantiteActuelle >= capaciteMax;
    }

    // Vérifier si la zone est vide
    public boolean estVide() {
        return quantiteActuelle == 0;
    }

    @Override
    public String toString() {
        return "ZoneStockage{" +
                "id='" + id + '\'' +
                ", libelle='" + libelle + '\'' +
                ", quantite=" + quantiteActuelle + "/" + capaciteMax +
                '}';
    }
}
