package com.example.indoorview.models;

import com.google.gson.annotations.SerializedName;

public class EspacioCouchDB {
    @SerializedName("id_espacio")
    public int id_espacio;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("url_imagenes")
    public String url_imagenes;

    @SerializedName("estado")
    public int estado;

    @SerializedName("geometria")
    public GeometriaCouchDB geometria;
}