package com.example.indoorview.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LugarCouchDB {
    @SerializedName("_id")
    public String _id;

    @SerializedName("_rev")
    public String _rev;
    @SerializedName("id_lugar")
    public int id_lugar;
    @SerializedName("id_piso")
    public int id_piso;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("url_imagenes")
    public String url_imagenes;

    @SerializedName("geojson")
    public List<List<List<Double>>> geojson; // Estructura: [[[lng, lat], ...]]

    @SerializedName("estado")
    public int estado;

    @SerializedName("color")
    public String color;

    @SerializedName("pisos")
    public List<PisoCouchDB> pisos;

    /**
     * Convertir geojson a formato esperado por la BD local
     * De: [[[lng, lat], [lng, lat], ...]]
     * A: "[[lng, lat], [lng, lat], ...]"
     */
    public String getGeojsonString() {
        if (geojson == null || geojson.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[[");

        List<List<Double>> coordenadas = geojson.get(0);
        for (int i = 0; i < coordenadas.size(); i++) {
            List<Double> punto = coordenadas.get(i);
            sb.append("[").append(punto.get(0)).append(",").append(punto.get(1)).append("]");
            if (i < coordenadas.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]]");
        return sb.toString();
    }
}