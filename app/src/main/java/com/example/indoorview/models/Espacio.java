package com.example.indoorview.models;

public class Espacio {

    private int id_espacio;
    private int id_lugar;
    private int id_piso;
    private String nombre;
    private String descripcion;
    private String url_imagenes;

    private int estado;

    public Espacio(int id_espacio, int id_lugar, int id_piso, String nombre, String descripcion, String url_imagenes, int estado) {
        this.id_espacio = id_espacio;
        this.id_lugar = id_lugar;
        this.id_piso = id_piso;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.url_imagenes = url_imagenes;
        this.estado = estado;
    }

    public int getId_espacio() {
        return id_espacio;
    }

    public void setId_espacio(int id_espacio) {
        this.id_espacio = id_espacio;
    }

    public int getId_lugar() {
        return id_lugar;
    }

    public void setId_lugar(int id_lugar) {
        this.id_lugar = id_lugar;
    }

    public int getId_piso() {
        return id_piso;
    }

    public void setId_piso(int id_piso) {
        this.id_piso = id_piso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUrl_imagenes() {
        return url_imagenes;
    }

    public void setUrl_imagenes(String url_imagenes) {
        this.url_imagenes = url_imagenes;
    }



    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }
}
