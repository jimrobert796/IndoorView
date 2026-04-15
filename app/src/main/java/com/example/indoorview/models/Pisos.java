package com.example.indoorview.models;

public class Pisos {
    private int id_piso;
    private int id_lugar;
    private int numero;
    private String nombre;
    private int activo;

    public Pisos(int id_piso, int id_lugar, int numero, String nombre, int activo) {
        this.id_piso = id_piso;
        this.id_lugar = id_lugar;
        this.numero = numero;
        this.nombre = nombre;
        this.activo = activo;
    }

    public int getId_piso() {
        return id_piso;
    }

    public void setId_piso(int id_piso) {
        this.id_piso = id_piso;
    }

    public int getId_lugar() {
        return id_lugar;
    }

    public void setId_lugar(int id_lugar) {
        this.id_lugar = id_lugar;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getActivo() {
        return activo;
    }

    public void setActivo(int activo) {
        this.activo = activo;
    }
}
