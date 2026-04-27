package com.example.indoorview;

import android.util.Log;
import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Lugar;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase para manejar búsquedas de lugares y espacios
 * Proporciona métodos de filtrado y búsqueda rápida
 */
public class SearchManager {

    private Database db;
    private List<SearchResult> resultados;

    // ════════════════════════════════════════════════════════════════
    // MODELO DE RESULTADO DE BÚSQUEDA
    // ════════════════════════════════════════════════════════════════
    public static class SearchResult {
        public int id;
        public String nombre;
        public String descripcion;
        public String tipo; // "LUGAR" o "ESPACIO"
        public String color;
        public String urlImagenes;
        public int idLugar; // Para espacios, el lugar padre
        public JsonObject jsonData; // Datos completos para el callback

        public SearchResult(int id, String nombre, String descripcion,
                            String tipo, String color, String urlImagenes,
                            int idLugar, JsonObject jsonData) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.tipo = tipo;
            this.color = color;
            this.urlImagenes = urlImagenes;
            this.idLugar = idLugar;
            this.jsonData = jsonData;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════
    public SearchManager(Database db) {
        this.db = db;
        this.resultados = new ArrayList<>();
    }

    // ════════════════════════════════════════════════════════════════
    // BÚSQUEDA PRINCIPAL
    // ════════════════════════════════════════════════════════════════

    /**
     * Realiza una búsqueda basada en texto
     * @param query Texto de búsqueda
     * @return Lista de resultados encontrados
     */
    public List<SearchResult> buscar(String query) {
        resultados.clear();

        if (query == null || query.trim().isEmpty()) {
            return resultados;
        }

        String queryLower = query.toLowerCase().trim();

        Log.d("SEARCH", "Buscando: " + queryLower);

        // 1️⃣ Buscar en LUGARES
        buscarEnLugares(queryLower);

        // 2️⃣ Buscar en ESPACIOS
        buscarEnEspacios(queryLower);

        Log.d("SEARCH", "Resultados encontrados: " + resultados.size());

        return resultados;
    }

    /**
     * Buscar en la lista de lugares
     */
    private void buscarEnLugares(String query) {
        List<Lugar> lugares = db.getLugares();

        for (Lugar lugar : lugares) {
            String nombre = lugar.getNombre().toLowerCase();
            String descripcion = lugar.getDescripcion().toLowerCase();

            // Búsqueda por nombre o descripción
            if (nombre.contains(query) || descripcion.contains(query)) {
                JsonObject data = new JsonObject();
                data.addProperty("id_lugar", lugar.getId_lugar());
                data.addProperty("nombre", lugar.getNombre());
                data.addProperty("descripcion", lugar.getDescripcion());
                data.addProperty("url_imagenes", lugar.getUrl_imagenes());
                data.addProperty("estado", lugar.getEstado());
                data.addProperty("geojson", lugar.getGeojson());
                data.addProperty("color", lugar.getColor());

                SearchResult result = new SearchResult(
                        lugar.getId_lugar(),
                        lugar.getNombre(),
                        lugar.getDescripcion(),
                        "LUGAR",
                        lugar.getColor(),
                        lugar.getUrl_imagenes(),
                        -1, // No tiene lugar padre
                        data
                );

                resultados.add(result);

                Log.d("SEARCH", "✓ Lugar encontrado: " + lugar.getNombre());
            }
        }
    }

    /**
     * Buscar en la lista de espacios
     */
    private void buscarEnEspacios(String query) {
        List<Lugar> lugares = db.getLugares();

        for (Lugar lugar : lugares) {
            List<Espacio> espacios = db.getEspaciosByLugar(lugar.getId_lugar());

            for (Espacio espacio : espacios) {
                String nombre = espacio.getNombre().toLowerCase();
                String descripcion = espacio.getDescripcion().toLowerCase();

                // Búsqueda por nombre o descripción
                if (nombre.contains(query) || descripcion.contains(query)) {

                    // Obtener geometría para datos completos
                    com.example.indoorview.models.Geometria geo =
                            db.getGeometriaByEspacio(espacio.getId_espacio());

                    String color = (geo != null) ? geo.getColor() : "#2196F3";

                    JsonObject data = new JsonObject();
                    data.addProperty("id_espacio", espacio.getId_espacio());
                    data.addProperty("id_geometria", geo != null ? geo.getId_geometria() : -1);
                    data.addProperty("id_lugar", espacio.getId_lugar());
                    data.addProperty("id_piso", espacio.getId_piso());
                    data.addProperty("nombre", espacio.getNombre());
                    data.addProperty("descripcion", espacio.getDescripcion());
                    data.addProperty("url_imagenes", espacio.getUrl_imagenes());
                    data.addProperty("estado", espacio.getEstado());
                    data.addProperty("vertices", geo != null ? geo.getVertices() : "");
                    data.addProperty("color", color);

                    SearchResult result = new SearchResult(
                            espacio.getId_espacio(),
                            espacio.getNombre(),
                            espacio.getDescripcion(),
                            "ESPACIO",
                            color,
                            espacio.getUrl_imagenes(),
                            espacio.getId_lugar(),
                            data
                    );

                    resultados.add(result);

                    Log.d("SEARCH", "✓ Espacio encontrado: " + espacio.getNombre() +
                            " (en " + lugar.getNombre() + ")");
                }
            }
        }
    }

    /**
     * Obtener todos los resultados actuales
     */
    public List<SearchResult> getResultados() {
        return new ArrayList<>(resultados);
    }

    /**
     * Limpiar resultados
     */
    public void limpiar() {
        resultados.clear();
    }

    /**
     * Obtener resultado por índice
     */
    public SearchResult getResultado(int index) {
        if (index >= 0 && index < resultados.size()) {
            return resultados.get(index);
        }
        return null;
    }
}