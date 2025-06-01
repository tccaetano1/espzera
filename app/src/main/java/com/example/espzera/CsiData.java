package com.example.espzera;

import java.io.Serializable;

public class CsiData implements Serializable {
    private int id;
    private int idLeitura; // ID da leitura à qual este dado CSI pertence
    private String timestamp; // Carimbo de data/hora da coleta do CSI
    private String csiPayload; // Os dados CSI em si (como String, pode ser JSON ou outro formato)

    // Construtor completo
    public CsiData(int id, int idLeitura, String timestamp, String csiPayload) {
        this.id = id;
        this.idLeitura = idLeitura;
        this.timestamp = timestamp;
        this.csiPayload = csiPayload;
    }

    // Construtor para adicionar novo (sem ID, ID será auto-incrementado)
    public CsiData(int idLeitura, String timestamp, String csiPayload) {
        this.idLeitura = idLeitura;
        this.timestamp = timestamp;
        this.csiPayload = csiPayload;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getIdLeitura() {
        return idLeitura;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getCsiPayload() {
        return csiPayload;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setIdLeitura(int idLeitura) {
        this.idLeitura = idLeitura;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setCsiPayload(String csiPayload) {
        this.csiPayload = csiPayload;
    }

    @Override
    public String toString() {
        return "CsiData{" +
                "id=" + id +
                ", idLeitura=" + idLeitura +
                ", timestamp='" + timestamp + '\'' +
                ", csiPayload='" + csiPayload + '\'' +
                '}';
    }
}
