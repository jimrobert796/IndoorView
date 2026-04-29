package com.example.indoorview.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeometriaCouchDB {
    @SerializedName("id_geometria")
    public int id_geometria;

    @SerializedName("id_espacio")
    public int id_espacio;

    @SerializedName("id_lugar")
    public int id_lugar;

    @SerializedName("id_piso")
    public int id_piso;

    @SerializedName("tipo")
    public String tipo;

    @SerializedName("vertices")
    public List<List<List<Double>>> vertices; // Estructura: [[[lng, lat], ...]]

    @SerializedName("color")
    public String color;

    /**
     * Convertir vértices a formato esperado por la BD local
     */
    public String getVerticesString() {
        if (vertices == null || vertices.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[[");

        List<List<Double>> coordenadas = vertices.get(0);
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
