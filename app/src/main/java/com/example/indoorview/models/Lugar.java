package com.example.indoorview.models;

public class Lugar {
    private int id_lugar;
    private String nombre;
    private String descripcion;
    private String url_imagenes;

    private String geojson;
    private int estado;

    public Lugar(int id_lugar, String nombre, String descripcion,String url_imagenes, String geojson, int estado) {

        this.id_lugar = id_lugar;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.url_imagenes = url_imagenes;
        this.geojson = geojson;
        this.estado = estado;
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
