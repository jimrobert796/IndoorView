package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Toast;

import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Lugar;
import com.google.gson.JsonObject;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MapManager {

    private MapView mapView;
    private Database db;
    private Context context;  // Añadir Context

    private PointAnnotationManager managerLugares;
    private PointAnnotationManager managerEspacios;

    private int lugarSeleccionado = -1;

    private int id_lugar;
    private int id_espacio;



    // Constructor que necesita el MapView, la instancia bd y el contexto en donde
    public MapManager(MapView mapView, Database db, Context context) {
        this.mapView = mapView;
        this.db = db;
        this.context = context;

        // Crear dos managers separados
        AnnotationPlugin plugin = mapView.getPlugin(Plugin.Mapbox.MAPBOX_ANNOTATION_PLUGIN_ID);

        managerLugares = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        managerEspacios = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        // =============== EVENTOS DE MAPA ==========================

        managerLugares.addClickListener(annotation -> {


            // Se me olvida que esto es en forma de lista
            // No confundir Objeto con ID de tabla
            Lugar lugar = db.getLugares().get(id_lugar -1);

            mostrarInfoLugar(lugar);

            // SIEMPRE limpiar
            limpiarEspacios();
            limpiarEspaciosDeLugar(id_lugar);


            lugarSeleccionado = -1;

            Toast.makeText(context, "Espacios ocultos", Toast.LENGTH_SHORT).show();

            return true;
        });

        // mantener presionado muestra los espacios
        managerLugares.addLongClickListener(annotation -> {

            // Si es OTRO lugar → cargar
            if (lugarSeleccionado != id_lugar) {

                // para eliminar no se usara aun pero estara ahi
                // managerLugares.delete(annotation);
                limpiarEspacios();
                limpiarEspaciosDeLugar(lugarSeleccionado);
                mostrarEspacios(id_lugar);

                lugarSeleccionado = id_lugar;

                Toast.makeText(context, "Mostrando espacios de: " + id_lugar, Toast.LENGTH_SHORT).show();

            } else {
                // Si es el mismo → ocultar
                limpiarEspacios();
                limpiarEspaciosDeLugar(id_lugar);

                lugarSeleccionado = -1;

                Toast.makeText(context, "Ocultando espacios", Toast.LENGTH_SHORT).show();
            }

            return true;
        });

        // PARA ESPACIOS
        managerEspacios.addClickListener(annotation -> {

            Espacio espacio = db.getEspacioById(id_espacio);
            Geometria geometria = db.getGeometriaByEspacio(id_espacio);

            mostrarInfoEspacio(espacio, geometria);

            Toast.makeText(context, "Espacio: " + annotation.getTextField(), Toast.LENGTH_SHORT).show();
            return true;
        });


    }

    // Verificar conexión (corregido) log: BD_CONEXION
    public void verificarConexionBD() {
        try {
            SQLiteDatabase testDb = db.getReadableDatabase();

            if (testDb != null && testDb.isOpen()) {
                Toast.makeText(context, "Base de datos conectada correctamente", Toast.LENGTH_SHORT).show();
                Log.d("BD_CONEXION", "Base de datos abierta correctamente");

                contarRegistros();
            } else {
                Toast.makeText(context, "Error: No se pudo conectar a la BD", Toast.LENGTH_LONG).show();
            }

            testDb.close();

        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("BD_CONEXION", "Error de conexión: " + e.getMessage());
        }
    }

    // Contar registros (corregido)
    private void contarRegistros() {
        List<Lugar> lugares = db.getLugares();

        StringBuilder mensaje = new StringBuilder();

        for (Lugar lugar : lugares) {
            mensaje.append(" ").append(lugar.getNombre()).append("\n");
            mensaje.append("    Coordenadas: ").append(lugar.getLatitud())
                    .append(", ").append(lugar.getLongitud()).append("\n");

            List<Espacio> espacios = db.getEspaciosByLugar(lugar.getId_lugar());
            for (Espacio espacio : espacios) {
                mensaje.append("    ").append(espacio.getNombre()).append("\n");
                mensaje.append("       Coordenadas: ").append(espacio.getLatitud())
                        .append(", ").append(espacio.getLongitud()).append("\n");
            }
            mensaje.append("\n");
        }

        // Usar context en lugar de this
        new AlertDialog.Builder(context)
                .setTitle("Datos IndoorView")
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }


    public void cargarPoligonosLugar() {
        List<Lugar> lugares = db.getLugares();

        if (lugares.isEmpty()) {
            Log.d("POLIGONOS", "No hay edificios");
            return;
        }

        for (Lugar lugar : lugares) {
            mapView.getMapboxMap().getStyle(style -> {
                dibujarPoligonoLugar(style, lugar);
            });
        }
    }

    public void dibujarPoligonoLugar(Style style, Lugar lugar) {
        String geojson = lugar.getGeojson();

        if (geojson == null || geojson.isEmpty()) {
            Log.d("POLIGONOS", "Edificio sin geojson: " + lugar.getNombre());
            return;
        }

        // Convertir a GeoJSON completo
        String geojsonCompleto = "{\"type\":\"Polygon\",\"coordinates\":" + geojson + "}";
        String sourceId = "edificio-" + lugar.getId_lugar();
        String color = "#2196F3";



        Log.d("POLIGONOS", " Dibujado: " + lugar.getGeojson());



        try {
            // Fuente
            HashMap<String, Value> sourceProps = new HashMap<>();
            sourceProps.put("type", Value.valueOf("geojson"));
            sourceProps.put("data", Value.valueOf(geojsonCompleto));
            style.addStyleSource(sourceId, new Value(sourceProps));

            // Capa de relleno
            HashMap<String, Value> fillLayerProps = new HashMap<>();
            fillLayerProps.put("id", Value.valueOf(sourceId + "-fill"));
            fillLayerProps.put("type", Value.valueOf("fill"));
            fillLayerProps.put("source", Value.valueOf(sourceId));

            HashMap<String, Value> fillPaint = new HashMap<>();
            fillPaint.put("fill-color", Value.valueOf(color));
            fillPaint.put("fill-opacity", Value.valueOf(0.1));
            fillLayerProps.put("paint", new Value(fillPaint));
            style.addStyleLayer(new Value(fillLayerProps), null);

            // Capa de líneas (borde)
            HashMap<String, Value> lineLayerProps = new HashMap<>();
            lineLayerProps.put("id", Value.valueOf(sourceId + "-line"));
            lineLayerProps.put("type", Value.valueOf("line"));
            lineLayerProps.put("source", Value.valueOf(sourceId));

            HashMap<String, Value> linePaint = new HashMap<>();
            linePaint.put("line-color", Value.valueOf(color));
            linePaint.put("line-width", Value.valueOf(0.5));
            lineLayerProps.put("paint", new Value(linePaint));
            style.addStyleLayer(new Value(lineLayerProps), null);

            style.addStyleLayer(new Value(fillLayerProps), null);

            Point centro = calcularCentroDesdeGeoJson(geojson);
            if (centro != null) {
                agregarPinLugar(
                        centro,
                        lugar.getNombre(),
                        "#0080ff",
                        lugar.getId_lugar()
                );
            }

            Log.d("POLIGONOS", "Dibujado: " + lugar.getNombre());




        } catch (Exception e) {
            Log.e("POLIGONOS", "Error: " + e.getMessage());
        }
    }



    public void dibujarTodosLosEspacios() {
        Log.d("ESPACIOS", "========== DIBUJANDO TODOS LOS ESPACIOS ==========");

        // Obtener todos los edificios
        List<Lugar> lugares = db.getLugares();

        if (lugares.isEmpty()) {
            Log.e("ESPACIOS", " No hay edificios en la base de datos");
            Toast.makeText(context, "No hay edificios", Toast.LENGTH_SHORT).show();
            return;
        }



        int totalEspacios = 0;
        int dibujados = 0;

        for (Lugar lugar : lugares) {
            Log.d("ESPACIOS", " Edificio: " + lugar.getNombre() + " (ID: " + lugar.getId_lugar() + ")");

            // Obtener espacios de este edificio
            List<Espacio> espacios = db.getEspaciosByLugar(lugar.getId_lugar());
            totalEspacios += espacios.size();

            for (Espacio espacio : espacios) {
                Log.d("ESPACIOS", "   Espacio: " + espacio.getNombre() + " (ID: " + espacio.getId_espacio() + ")");

                // Obtener geometría del espacio
                Geometria geometria = db.getGeometriaByEspacio(espacio.getId_espacio());


                if (geometria != null && geometria.getVertices() != null && !geometria.getVertices().isEmpty()) {
                    Log.d("ESPACIOS", "      Tiene geometría");
                    Log.d("ESPACIOS", "      Vértices: " + geometria.getVertices().substring(0, Math.min(80, geometria.getVertices().length())));
                    Log.d("ESPACIOS", "      Color: " + geometria.getColor());

                    // Dibujar el polígono
                    dibujarPoligonoEspacio(geometria, espacio);
                    dibujados++;
                } else {
                    Log.e("ESPACIOS", "      Sin geometría para: " + espacio.getNombre());
                }
            }
        }

        Log.d("ESPACIOS", "========== RESUMEN ==========");
        Log.d("ESPACIOS", "Total espacios: " + totalEspacios);
        Log.d("ESPACIOS", "Dibujados: " + dibujados);
        Log.d("ESPACIOS", "Sin geometría: " + (totalEspacios - dibujados));

        Toast.makeText(context, "Dibujados " + dibujados + " de " + totalEspacios + " espacios", Toast.LENGTH_LONG).show();
    }

    // ==========================================
// DIBUJAR UN ESPACIO INDIVIDUAL
// ==========================================
    public void dibujarPoligonoEspacio(Geometria geometria, Espacio espacio) {
        String vertices = geometria.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            Log.e("DIBUJO", " Vertices vacíos para: " + espacio.getNombre());
            return;
        }

        // Convertir a GeoJSON
        String geojson = "{\"type\":\"Polygon\",\"coordinates\":" + vertices + "}";
        String sourceId = "espacio-" + espacio.getId_espacio();
        String color = geometria.getColor();



        final String finalColor = color;
        final String finalSourceId = sourceId;
        final String finalGeojson = geojson;

        mapView.getMapboxMap().getStyle(style -> {
            try {
                // Fuente GeoJSON
                HashMap<String, Value> sourceProps = new HashMap<>();
                sourceProps.put("type", Value.valueOf("geojson"));
                sourceProps.put("data", Value.valueOf(finalGeojson));
                style.addStyleSource(finalSourceId, new Value(sourceProps));

                // Capa de relleno
                HashMap<String, Value> fillLayerProps = new HashMap<>();
                fillLayerProps.put("id", Value.valueOf(finalSourceId + "-fill"));
                fillLayerProps.put("type", Value.valueOf("fill"));
                fillLayerProps.put("source", Value.valueOf(finalSourceId));

                HashMap<String, Value> fillPaint = new HashMap<>();
                fillPaint.put("fill-color", Value.valueOf(finalColor));
                fillPaint.put("fill-opacity", Value.valueOf(0.1));
                fillLayerProps.put("paint", new Value(fillPaint));
                style.addStyleLayer(new Value(fillLayerProps), null);

                // Capa de líneas (borde)
                HashMap<String, Value> lineLayerProps = new HashMap<>();
                lineLayerProps.put("id", Value.valueOf(finalSourceId + "-line"));
                lineLayerProps.put("type", Value.valueOf("line"));
                lineLayerProps.put("source", Value.valueOf(finalSourceId));

                HashMap<String, Value> linePaint = new HashMap<>();
                linePaint.put("line-color", Value.valueOf("#000000"));
                linePaint.put("line-width", Value.valueOf(0.5));
                lineLayerProps.put("paint", new Value(linePaint));
                style.addStyleLayer(new Value(lineLayerProps), null);

                Point centro = calcularCentroDesdeGeoJson(vertices);
                if (centro != null) {
                    agregarPinEspacio(
                            centro,
                            espacio.getNombre(),
                            "#0080ff",
                            espacio.getId_espacio()
                    );
                }

                Log.d("DIBUJO", " Dibujado: " + espacio.getNombre());

            } catch (Exception e) {
                Log.e("DIBUJO", " Error dibujando " + espacio.getNombre() + ": " + e.getMessage());
            }
        });
    }




    // Bitmaps cambiara a futuro para los iconos de wilfredo
    private Bitmap crearPinBitmap(String hexColor) {
        int size = 60;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint sombra = new Paint(Paint.ANTI_ALIAS_FLAG);
        sombra.setColor(Color.parseColor("#44000000"));
        canvas.drawCircle(size / 2f + 2, size / 2f + 2, size / 2.5f, sombra);

        Paint exterior = new Paint(Paint.ANTI_ALIAS_FLAG);
        exterior.setColor(Color.parseColor(hexColor));
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, exterior);

        Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
        borde.setColor(Color.WHITE);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(3f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, borde);

        Paint interior = new Paint(Paint.ANTI_ALIAS_FLAG);
        interior.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 6f, interior);

        return bitmap;
    }

    // crear punto bitmap
    private Bitmap crearPuntoBitmap(String hexColor) {
        int size = 30;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor(hexColor));
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint);

        Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
        borde.setColor(Color.WHITE);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(3f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, borde);

        return bitmap;
    }

    // Este contendra informacion para saber cual hacer display

    private void agregarPinLugar(Point punto, String texto, String color, int idLugar) {
        Bitmap icono = crearPinBitmap(color);
        PointAnnotationOptions op = new PointAnnotationOptions()
                .withPoint(punto)
                .withIconImage(icono)
                .withIconSize(0.9)
                .withTextField(texto)
                .withTextSize(11.0)
                .withIconAnchor(IconAnchor.CENTER)
                .withTextColor("#000000")
                .withTextHaloColor("#ffffff")
                .withTextHaloWidth(2.0)
                .withTextOffset(Arrays.asList(0.0, 1.5));
        id_lugar = idLugar;
        managerLugares.create(op);

    }

    private void agregarPinEspacio(Point punto, String texto, String color, int idEspacio) {
        Bitmap icono = crearPinBitmap(color);
        PointAnnotationOptions op = new PointAnnotationOptions()
                .withPoint(punto)
                .withIconImage(icono)
                .withIconSize(0.9)
                .withTextField(texto)
                .withTextSize(11.0)
                .withIconAnchor(IconAnchor.CENTER)
                .withTextColor("#000000")
                .withTextHaloColor("#ffffff")
                .withTextHaloWidth(2.0)
                .withTextOffset(Arrays.asList(0.0, 1.5));
        id_espacio = idEspacio;
        managerEspacios.create(op);
    }
    public void limpiarEspacios() {
        managerEspacios.deleteAll();
    }

    public void limpiarLugares(){
        managerLugares.deleteAll();
    }

    public void limpiarTodo() {
        managerEspacios.deleteAll();
        managerLugares.deleteAll();
    }


    public void limpiarEspaciosDeLugar(int idLugar) {
        mapView.getMapboxMap().getStyle(style -> {

            List<Espacio> espacios = db.getEspaciosByLugar(idLugar);

            for (Espacio e : espacios) {
                String id = "espacio-" + e.getId_espacio();

                if (style.styleLayerExists(id + "-fill")) {
                    style.removeStyleLayer(id + "-fill");
                }
                if (style.styleLayerExists(id + "-line")) {
                    style.removeStyleLayer(id + "-line");
                }
                if (style.styleSourceExists(id)) {
                    style.removeStyleSource(id);
                }
            }
        });
    }
    private void mostrarEspacios(int idLugar) {

        List<Espacio> espacios = db.getEspaciosByLugar(idLugar);

        if (espacios.isEmpty()) {
            Toast.makeText(context, "No hay espacios", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Espacio espacio : espacios) {
            Geometria geo = db.getGeometriaByEspacio(espacio.getId_espacio());

            if (geo != null) {
                dibujarPoligonoEspacio(geo, espacio);
            }
        }
    }
    private Point calcularCentro(List<Point> puntos) {
        double sumLng = 0, sumLat = 0;
        int total = puntos.size() - 1;
        for (int i = 0; i < total; i++) {
            sumLng += puntos.get(i).longitude();
            sumLat += puntos.get(i).latitude();
        }
        return Point.fromLngLat(sumLng / total, sumLat / total);
    }

    private Point calcularCentroDesdeGeoJson(String geojson) {
        try {
            // Tomar solo el PRIMER polígono
            int firstEnd = geojson.indexOf("]]]");
            if (firstEnd != -1) {
                geojson = geojson.substring(0, firstEnd + 3);
            }

            String clean = geojson
                    .replace("[[[", "")
                    .replace("]]]", "")
                    .trim();

            String[] puntosStr = clean.split("\\],\\[");

            // eliminar último repetido
            if (puntosStr.length > 1) {
                puntosStr = java.util.Arrays.copyOf(puntosStr, puntosStr.length - 1);
            }

            double sumLng = 0;
            double sumLat = 0;

            for (String punto : puntosStr) {
                String[] coords = punto.split(",");
                sumLng += Double.parseDouble(coords[0].trim());
                sumLat += Double.parseDouble(coords[1].trim());
            }

            int total = puntosStr.length;

            return Point.fromLngLat(sumLng / total, sumLat / total);

        } catch (Exception e) {
            Log.e("CENTRO", "Error: " + e.getMessage());
            return null;
        }
    }


    ///  ========= CUADROS DE DIALOGO PARA INFORMACION =======


    private void mostrarInfoLugar(Lugar lugar) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("INFORMACION DEL EDIFICIO\n\n");
        mensaje.append("Nombre: ").append(lugar.getNombre()).append("\n");
        mensaje.append("ID: ").append(lugar.getId_lugar()).append("\n");
        mensaje.append("Descripcion: ").append(lugar.getDescripcion()).append("\n");
        mensaje.append("Latitud: ").append(lugar.getLatitud()).append("\n");
        mensaje.append("Longitud: ").append(lugar.getLongitud()).append("\n");
        mensaje.append("geojson: ").append(lugar.getGeojson()).append("\n");
        mensaje.append("estado: ").append(lugar.getEstado()).append("\n");

        if (lugar.getGeojson() != null && !lugar.getGeojson().isEmpty()) {
            String geojson = lugar.getGeojson();
            if (geojson.length() > 200) {
                geojson = geojson.substring(0, 200) + "...";
            }
            mensaje.append("GeoJSON: ").append(geojson);
        } else {
            mensaje.append("GeoJSON: No disponible");
        }

        new AlertDialog.Builder(context)
                .setTitle("Informacion: " + lugar.getNombre())
                .setMessage(mensaje.toString())
                .setNegativeButton("Cerrar", null)
                .show();
    }


// DIALOGO PARA MOSTRAR INFORMACION DE UN ESPACIO (AULA)

    private void mostrarInfoEspacio(Espacio espacio, Geometria geometria) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("INFORMACION DEL ESPACIO\n\n");
        mensaje.append("Nombre: ").append(espacio.getNombre()).append("\n");
        mensaje.append("ID: ").append(espacio.getId_espacio()).append("\n");
        mensaje.append("Descripcion: ").append(espacio.getDescripcion()).append("\n");
        mensaje.append("Latitud: ").append(espacio.getLatitud()).append("\n");
        mensaje.append("Longitud: ").append(espacio.getLongitud()).append("\n");
        mensaje.append("Estado: ").append(espacio.getEstado()).append("\n");

        if (geometria != null) {
            mensaje.append("Color geometria: ").append(geometria.getColor()).append("\n");

            String vertices = geometria.getVertices();
            if (vertices != null && !vertices.isEmpty()) {
                if (vertices.length() > 200) {
                    vertices = vertices.substring(0, 200) + "...";
                }
                mensaje.append("Vertices: ").append(vertices);
            } else {
                mensaje.append("Vertices: No disponibles");
            }
        } else {
            mensaje.append("Sin informacion de geometria");
        }

        new AlertDialog.Builder(context)
                .setTitle("Informacion: " + espacio.getNombre())
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }





}