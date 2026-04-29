package com.example.indoorview.models;

public class Detalle {
        private int id_detalle;
        private String nombre;
        private String tipo;      // "linea", "poligono", "circulo", etc.
        private String geojson;   // Coordenadas en formato GeoJSON
        private String color;     // Color de línea/borde
        private String fill_color; // Color de relleno (para polígonos)
        private Double fill_opacity; // Color de relleno (para polígonos)

    public Detalle(int id_detalle, String nombre, String tipo, String geojson, String color, String fill_color, Double fill_opacity) {
        this.id_detalle = id_detalle;
        this.nombre = nombre;
        this.tipo = tipo;
        this.geojson = geojson;
        this.color = color;
        this.fill_color = fill_color;
        this.fill_opacity = fill_opacity;
    }

    public int getId_detalle() {
        return id_detalle;
    }

    public void setId_detalle(int id_detalle) {
        this.id_detalle = id_detalle;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getGeojson() {
        return geojson;
    }

    public void setGeojson(String geojson) {
        this.geojson = geojson;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getFill_color() {
        return fill_color;
    }

    public void setFill_color(String fill_color) {
        this.fill_color = fill_color;
    }

    public Double getFill_opacity() {
        return fill_opacity;
    }

    public void setFill_opacity(Double fill_opacity) {
        this.fill_opacity = fill_opacity;
    }
}
