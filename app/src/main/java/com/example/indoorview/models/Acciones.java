package com.example.indoorview.models;

public class Acciones {
    private int id_accion;
    private int nombre;

    public Acciones(int id_accion, int nombre) {
        this.id_accion = id_accion;
        this.nombre = nombre;
    }

    public int getId_accion() {
        return id_accion;
    }

    public void setId_accion(int id_accion) {
        this.id_accion = id_accion;
    }

    public int getNombre() {
        return nombre;
    }

    public void setNombre(int nombre) {
        this.nombre = nombre;
    }
}
