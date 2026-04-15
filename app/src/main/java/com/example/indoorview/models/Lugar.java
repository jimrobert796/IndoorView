package com.example.indoorview.models;

public class Lugar {
    private int id_lugar;
    private String nombre;
    private String descripcion;
    private String url_imagenes;
    private String latitud;
    private String longitud;
    private String geojson;
    private int estado;

    public Lugar(int estado, String geojson, String longitud, String latitud, String url_imagenes, String descripcion, String nombre, int id_lugar) {
        this.estado = estado;
        this.geojson = geojson;
        this.longitud = longitud;
        this.latitud = latitud;
        this.url_imagenes = url_imagenes;
        this.descripcion = descripcion;
        this.nombre = nombre;
        this.id_lugar = id_lugar;
    }

    public int getId_lugar() {
        return id_lugar;
    }

    public void setId_lugar(int id_lugar) {
        this.id_lugar = id_lugar;
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

    public String getLatitud() {
        return latitud;
    }

    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    public String getLongitud() {
        return longitud;
    }

    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }

    public String getGeojson() {
        return geojson;
    }

    public void setGeojson(String geojson) {
        this.geojson = geojson;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }




}
