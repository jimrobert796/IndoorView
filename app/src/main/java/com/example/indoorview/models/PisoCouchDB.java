package com.example.indoorview.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PisoCouchDB {
    @SerializedName("id_piso")
    public int id_piso;

    @SerializedName("numero")
    public int numero;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("activo")
    public int activo;

    @SerializedName("espacios")
    public List<EspacioCouchDB> espacios;
}