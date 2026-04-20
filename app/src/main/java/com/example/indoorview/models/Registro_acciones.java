package com.example.indoorview.models;

public class Registro_acciones {
    private int id_usuario;
    private int id_lugar;
    private int id_espacio;
    private int id_evento;
    private int id_accion;
    private String descripcion;
    private String fecha_hora;

    public Registro_acciones(int id_usuario, int id_lugar, int id_espacio, int id_evento, int id_accion, String descripcion, String fecha_hora) {
        this.id_usuario = id_usuario;
        this.id_lugar = id_lugar;
        this.id_espacio = id_espacio;
        this.id_evento = id_evento;
        this.id_accion = id_accion;
        this.descripcion = descripcion;
        this.fecha_hora = fecha_hora;
    }

    public int getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(int id_usuario) {
        this.id_usuario = id_usuario;
    }

    public int getId_lugar() {
        return id_lugar;
    }

    public void setId_lugar(int id_lugar) {
        this.id_lugar = id_lugar;
    }

    public int getId_espacio() {
        return id_espacio;
    }

    public void setId_espacio(int id_espacio) {
        this.id_espacio = id_espacio;
    }

    public int getId_evento() {
        return id_evento;
    }

    public void setId_evento(int id_evento) {
        this.id_evento = id_evento;
    }

    public int getId_accion() {
        return id_accion;
    }

    public void setId_accion(int id_accion) {
        this.id_accion = id_accion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFecha_hora() {
        return fecha_hora;
    }

    public void setFecha_hora(String fecha_hora) {
        this.fecha_hora = fecha_hora;
    }
}
