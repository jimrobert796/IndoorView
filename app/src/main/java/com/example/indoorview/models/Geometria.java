package com.example.indoorview.models;

public class Geometria {
    private int id_geometria;

    private int id_espacio ;
    private int id_lugar;

    private int id_piso;

    private String tipo;          // "Polygon", "Point", "LineString"
    private String vertices;      // JSON con las coordenadas del polígono
    private String color;         // Color en formato hexadecimal (ej: "#0080ff")

    public Geometria(int id_geometria, int id_espacio, int id_lugar, int id_piso, String tipo, String vertices, String color) {
        this.id_geometria = id_geometria;
        this.id_piso = id_piso;
        this.id_espacio = id_espacio;
        this.id_lugar = id_lugar;
        this.tipo = tipo;
        this.vertices = vertices;
        this.color = color;
    }

    public int getId_geometria() {
        return id_geometria;
    }

    public void setId_geometria(int id_geometria) {
        this.id_geometria = id_geometria;
    }

    public int getId_espacio() {
        return id_espacio;
    }

    public void setId_espacio(int id_espacio) {
        this.id_espacio = id_espacio;
    }

    public int getId_piso() {
        return id_piso;
    }

    public void setId_piso(int id_piso) {
        this.id_piso = id_piso;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getVertices() {
        return vertices;
    }

    public void setVertices(String vertices) {
        this.vertices = vertices;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getId_lugar() {
        return id_lugar;
    }

    public void setId_lugar(int id_lugar) {
        this.id_lugar = id_lugar;
    }
}
