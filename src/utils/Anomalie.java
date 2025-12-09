package utils;

import java.util.Date;

public class Anomalie {
    private int id;
    private Date timestamp;
    private String source;        // ID de la machine
    private String description;

    //Constructeur
     public Anomalie(int id, Date timestamp, String source, String description) {
        this.id = id;
        this.timestamp = timestamp;
        this.source = source;
        this.description = description;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Anomalie{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
