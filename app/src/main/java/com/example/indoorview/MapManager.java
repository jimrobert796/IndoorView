package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Lugar;
import com.example.indoorview.models.Pisos;
import com.google.gson.JsonObject;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MapManager {

    private MapView mapView;
    private Database db;
    private Context context;

    private PointAnnotationManager managerLugares;
    private PointAnnotationManager managerEspacios;
    private PointAnnotationManager managerTemporal;
    private PointAnnotationManager managerPermanente;

    public int lugarSeleccionado = -1;
    public boolean modoEdicion = false;

    private List<String> capasEspaciosRegistradas = new ArrayList<>();
    private List<String> capasLugaresRegistradas = new ArrayList<>();
    List<PointAnnotation> pinesOcultos = new ArrayList<>();
    private PointAnnotation pinOcultoActual = null;

    Spinner spinnerPisos;

    // ════════════════════════════════════════════════════════════════
    // VARIABLES PARA MODO EDICIÓN (AGREGAR LUGARES Y ESPACIOS)
    // ════════════════════════════════════════════════════════════════
    private List<Point> puntosActuales = new ArrayList<>();
    private List<Point> puntosLugarActual = new ArrayList<>();
    private int lugarActualId = 0;
    private int espacioContador = 0;

    // ════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════
    public MapManager(MapView mapView, Database db, Context context, Spinner spinner) {
        this.mapView = mapView;
        this.db = db;
        this.context = context;
        this.spinnerPisos = spinner;

        if (spinnerPisos == null) {
            Log.e("SPINNER_ERROR", "¡El spinner es NULL en MapManager!");
        } else {
            Log.d("SPINNER_DEBUG", "✓ Spinner inicializado correctamente");
        }

        // Crear managers de anotaciones
        AnnotationPlugin plugin = mapView.getPlugin(Plugin.Mapbox.MAPBOX_ANNOTATION_PLUGIN_ID);

        managerLugares = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        managerEspacios = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        managerTemporal = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        managerPermanente = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        // ════════════════════════════════════════════════════════════════
        // EVENTOS DE MAPA - Click en LUGARES
        // ════════════════════════════════════════════════════════════════
        managerLugares.addClickListener(annotation -> {
            Point punto = annotation.getPoint();
            redireccionPin(punto);
            JsonObject data = (JsonObject) annotation.getData();
            // Limpiamos los espacios por si acaso
            limpiarEspacios();
            restaurarPinesOcultos();

            if (modoEdicion) {
                Toast.makeText(context, "Listo para modificar CRUD", Toast.LENGTH_SHORT).show();
            } else {
                mostrarInfoLugar(data);
                limpiarEspacios();
                lugarSeleccionado = -1;
                Toast.makeText(context, "Espacios ocultos", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // ════════════════════════════════════════════════════════════════
        // EVENTOS DE MAPA - Long Click en LUGARES (mostrar espacios)
        // ════════════════════════════════════════════════════════════════
        managerLugares.addLongClickListener(annotation -> {
            Point punto = annotation.getPoint();
            redireccionPin(punto);
            JsonObject data = (JsonObject) annotation.getData();

            if (data != null) {
                int idLugar = data.get("id_lugar").getAsInt();

                if (pinOcultoActual != null) {
                    pinOcultoActual.setIconSize(0.9);
                    pinOcultoActual.setTextOpacity(1.0);
                    managerLugares.update(pinOcultoActual);
                    pinOcultoActual = null;
                }

                if (lugarSeleccionado != idLugar) {
                    pinesOcultos.add(annotation);
                    annotation.setIconSize(0.0);
                    annotation.setTextOpacity(0.0);
                    managerLugares.update(annotation);
                    pinOcultoActual = annotation;

                    limpiarEspacios();
                    limpiarEspaciosDeLugar(lugarSeleccionado);
                    cargarPisos(idLugar);

                    lugarSeleccionado = idLugar;
                    Toast.makeText(context, "Mostrando espacios del lugar", Toast.LENGTH_SHORT).show();
                } else {
                    limpiarEspacios();
                    lugarSeleccionado = -1;
                    Toast.makeText(context, "Ocultando espacios", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Error: Datos no disponibles", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // ════════════════════════════════════════════════════════════════
        // EVENTOS DE MAPA - Click en ESPACIOS
        // ════════════════════════════════════════════════════════════════
        managerEspacios.addClickListener(annotation -> {
            Point punto = annotation.getPoint();
            redireccionPin(punto);
            JsonObject data = (JsonObject) annotation.getData();

            if (modoEdicion) {
                Toast.makeText(context, "Listo para modificar CRUD", Toast.LENGTH_SHORT).show();
            } else {
                mostrarInfoEspacio(data);
            }
            return true;
        });
    }

    // ════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS GENERALES
    // ════════════════════════════════════════════════════════════════

    public void setModoEdicion(boolean activar) {
        this.modoEdicion = activar;
    }

    public boolean isModoEdicion() {
        return modoEdicion;
    }

    private void restaurarPinesOcultos() {
        for (PointAnnotation pin : pinesOcultos) {
            pin.setIconSize(0.9);
            pin.setTextOpacity(1.0);
            managerLugares.update(pin);
        }
        pinesOcultos.clear();
    }

    private void redireccionPin(Point punto) {
        CameraAnimationsPlugin animationPlugin =
                mapView.getPlugin(Plugin.MAPBOX_CAMERA_PLUGIN_ID);

        animationPlugin.easeTo(
                new CameraOptions.Builder()
                        .center(punto)
                        .zoom(19.8)
                        .build(),
                new MapAnimationOptions.Builder()
                        .duration(1200L)
                        .build(),
                null
        );
    }

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

    private void contarRegistros() {
        List<Lugar> lugares = db.getLugares();
        StringBuilder mensaje = new StringBuilder();

        for (Lugar lugar : lugares) {
            mensaje.append(" ").append(lugar.getNombre()).append("\n");
            List<Espacio> espacios = db.getEspaciosByLugar(lugar.getId_lugar());
            for (Espacio espacio : espacios) {
                mensaje.append("    ").append(espacio.getNombre()).append("\n");
            }
            mensaje.append("\n");
        }

        new AlertDialog.Builder(context)
                .setTitle("Datos IndoorView")
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    // ════════════════════════════════════════════════════════════════
    // CARGAR Y DIBUJAR POLÍGONOS EXISTENTES (LECTURAS)
    // ════════════════════════════════════════════════════════════════

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

        String geojsonCompleto = "{\"type\":\"Polygon\",\"coordinates\":" + geojson + "}";
        String sourceId = "edificio-" + lugar.getId_lugar();

        String color;
        try {
            color = lugar.getColor();
        } catch (Exception e) {
            color = "#2196F3";
        }

        if (!capasLugaresRegistradas.contains(sourceId)) {
            capasLugaresRegistradas.add(sourceId);
        }

        try {
            HashMap<String, Value> sourceProps = new HashMap<>();
            sourceProps.put("type", Value.valueOf("geojson"));
            sourceProps.put("data", Value.valueOf(geojsonCompleto));
            style.addStyleSource(sourceId, new Value(sourceProps));

            HashMap<String, Value> fillLayerProps = new HashMap<>();
            fillLayerProps.put("id", Value.valueOf(sourceId + "-fill"));
            fillLayerProps.put("type", Value.valueOf("fill"));
            fillLayerProps.put("source", Value.valueOf(sourceId));

            HashMap<String, Value> fillPaint = new HashMap<>();
            fillPaint.put("fill-color", Value.valueOf(color));
            fillPaint.put("fill-opacity", Value.valueOf(0.1));
            fillLayerProps.put("paint", new Value(fillPaint));
            style.addStyleLayer(new Value(fillLayerProps), null);

            HashMap<String, Value> lineLayerProps = new HashMap<>();
            lineLayerProps.put("id", Value.valueOf(sourceId + "-line"));
            lineLayerProps.put("type", Value.valueOf("line"));
            lineLayerProps.put("source", Value.valueOf(sourceId));

            HashMap<String, Value> linePaint = new HashMap<>();
            linePaint.put("line-color", Value.valueOf(color));
            linePaint.put("line-width", Value.valueOf(0.5));
            lineLayerProps.put("paint", new Value(linePaint));
            style.addStyleLayer(new Value(lineLayerProps), null);

            Point centro = calcularCentroDesdeGeoJson(geojson);
            if (centro != null) {
                agregarPinLugar(centro, lugar.getNombre(), "#0080ff", lugar);
            }

            Log.d("POLIGONOS", "Dibujado: " + lugar.getNombre());

        } catch (Exception e) {
            Log.e("POLIGONOS", "Error: " + e.getMessage());
        }
    }

    public void dibujarTodosLosEspacios() {
        Log.d("ESPACIOS", "========== DIBUJANDO TODOS LOS ESPACIOS ==========");

        List<Lugar> lugares = db.getLugares();

        if (lugares.isEmpty()) {
            Log.e("ESPACIOS", "No hay edificios en la base de datos");
            Toast.makeText(context, "No hay edificios", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalEspacios = 0;
        int dibujados = 0;

        for (Lugar lugar : lugares) {
            Log.d("ESPACIOS", "Edificio: " + lugar.getNombre() + " (ID: " + lugar.getId_lugar() + ")");

            List<Espacio> espacios = db.getEspaciosByLugar(lugar.getId_lugar());
            totalEspacios += espacios.size();

            for (Espacio espacio : espacios) {
                Log.d("ESPACIOS", "   Espacio: " + espacio.getNombre() + " (ID: " + espacio.getId_espacio() + ")");

                Geometria geometria = db.getGeometriaByEspacio(espacio.getId_espacio());

                if (geometria != null && geometria.getVertices() != null && !geometria.getVertices().isEmpty()) {
                    Log.d("ESPACIOS", "      Tiene geometría");
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

        Toast.makeText(context, "Dibujados " + dibujados + " de " + totalEspacios + " espacios", Toast.LENGTH_LONG).show();
    }

    public void dibujarPoligonoEspacio(Geometria geometria, Espacio espacio) {
        String vertices = geometria.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            Log.e("DIBUJO", "Vertices vacíos para: " + espacio.getNombre());
            return;
        }

        String geojson = "{\"type\":\"Polygon\",\"coordinates\":" + vertices + "}";
        String sourceId = "espacio-" + espacio.getId_espacio();
        String color;

        try {
            color = geometria.getColor();
        } catch (Exception e) {
            color = "#2196F3";
        }

        if (!capasEspaciosRegistradas.contains(sourceId)) {
            capasEspaciosRegistradas.add(sourceId);
        }

        final String finalColor = color;
        final String finalSourceId = sourceId;
        final String finalGeojson = geojson;

        mapView.getMapboxMap().getStyle(style -> {
            try {
                HashMap<String, Value> sourceProps = new HashMap<>();
                sourceProps.put("type", Value.valueOf("geojson"));
                sourceProps.put("data", Value.valueOf(finalGeojson));
                style.addStyleSource(finalSourceId, new Value(sourceProps));

                HashMap<String, Value> fillLayerProps = new HashMap<>();
                fillLayerProps.put("id", Value.valueOf(finalSourceId + "-fill"));
                fillLayerProps.put("type", Value.valueOf("fill"));
                fillLayerProps.put("source", Value.valueOf(finalSourceId));

                HashMap<String, Value> fillPaint = new HashMap<>();
                fillPaint.put("fill-color", Value.valueOf(finalColor));
                fillPaint.put("fill-opacity", Value.valueOf(0.1));
                fillLayerProps.put("paint", new Value(fillPaint));
                style.addStyleLayer(new Value(fillLayerProps), null);

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
                    agregarPinEspacio(centro, espacio.getNombre(), "#0080ff", espacio, vertices);
                }

                Log.d("DIBUJO", "Dibujado: " + espacio.getNombre());

            } catch (Exception e) {
                Log.e("DIBUJO", "Error dibujando " + espacio.getNombre() + ": " + e.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    // MÉTODOS DE PINES Y BITMAPS
    // ════════════════════════════════════════════════════════════════

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

    public Bitmap crearPuntoBitmap(String hexColor) {
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

    private void agregarPinLugar(Point punto, String texto, String color, Lugar lugar) {
        Bitmap icono = crearPinBitmap(color);

        JsonObject data = new JsonObject();
        data.addProperty("id_lugar", lugar.getId_lugar());
        data.addProperty("nombre", lugar.getNombre());
        data.addProperty("descripcion", lugar.getDescripcion());
        data.addProperty("url_imagenes", lugar.getUrl_imagenes());
        data.addProperty("estado", lugar.getEstado());
        data.addProperty("geojson", lugar.getGeojson());
        data.addProperty("color", lugar.getColor());

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
                .withTextOffset(Arrays.asList(0.0, 1.5))
                .withData(data);
        managerLugares.create(op);
    }

    private void agregarPinEspacio(Point punto, String texto, String color, Espacio espacio, String vertices) {
        JsonObject data = new JsonObject();
        data.addProperty("id_espacio", espacio.getId_espacio());
        data.addProperty("id_lugar", espacio.getId_lugar());
        data.addProperty("id_piso", espacio.getId_piso());
        data.addProperty("nombre", espacio.getNombre());
        data.addProperty("descripcion", espacio.getDescripcion());
        data.addProperty("url_imagenes", espacio.getUrl_imagenes());
        data.addProperty("estado", espacio.getEstado());
        data.addProperty("vertices", vertices);

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
                .withTextOffset(Arrays.asList(0.0, 1.5))
                .withData(data);
        managerEspacios.create(op);
    }

    // ════════════════════════════════════════════════════════════════
    // LIMPIEZA DE ELEMENTOS DEL MAPA
    // ════════════════════════════════════════════════════════════════

    public void limpiarEspacios() {
        managerEspacios.deleteAll();

        mapView.getMapboxMap().getStyle(style -> {
            for (String sourceId : capasEspaciosRegistradas) {
                if (style.styleLayerExists(sourceId + "-fill")) {
                    style.removeStyleLayer(sourceId + "-fill");
                }
                if (style.styleLayerExists(sourceId + "-line")) {
                    style.removeStyleLayer(sourceId + "-line");
                }
                if (style.styleSourceExists(sourceId)) {
                    style.removeStyleSource(sourceId);
                }
            }
            capasEspaciosRegistradas.clear();
        });
    }

    public void limpiarLugares() {
        managerLugares.deleteAll();

        mapView.getMapboxMap().getStyle(style -> {
            for (String sourceId : capasLugaresRegistradas) {
                if (style.styleLayerExists(sourceId + "-fill")) {
                    style.removeStyleLayer(sourceId + "-fill");
                }
                if (style.styleLayerExists(sourceId + "-line")) {
                    style.removeStyleLayer(sourceId + "-line");
                }
                if (style.styleSourceExists(sourceId)) {
                    style.removeStyleSource(sourceId);
                }
            }
            capasLugaresRegistradas.clear();
        });
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

    // ════════════════════════════════════════════════════════════════
    // INFORMACIÓN DE LUGARES Y ESPACIOS (DIÁLOGOS)
    // ════════════════════════════════════════════════════════════════

    private void mostrarLugares() {
        List<Lugar> lugares = db.getLugares();

        if (lugares.isEmpty()) {
            Toast.makeText(context, "No hay lugares activos", Toast.LENGTH_SHORT).show();
            return;
        }

        int contador = 0;
        for (Lugar lugar : lugares) {
            if (lugar.getEstado() == 1) {
                mapView.getMapboxMap().getStyle(style -> {
                    dibujarPoligonoLugar(style, lugar);
                });
                contador++;
            }
        }

        Toast.makeText(context, "Mostrados " + contador + " lugares activos", Toast.LENGTH_SHORT).show();
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

    private void mostrarInfoLugar(JsonObject data) {
        int idLugar = data.get("id_lugar").getAsInt();
        String nombre = data.get("nombre").getAsString();
        String descripcion = data.get("descripcion").getAsString();
        String url_imagenes = data.get("url_imagenes").getAsString();
        int estado = data.get("estado").getAsInt();
        String geojson = data.get("geojson").getAsString();
        String color = data.get("color").getAsString();

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("INFORMACIÓN DEL LUGAR\n\n");
        mensaje.append("Nombre: ").append(nombre).append("\n");
        mensaje.append("Descripción: ").append(descripcion).append("\n");
        mensaje.append("Imágenes: ").append(url_imagenes).append("\n");
        mensaje.append("ID Lugar: ").append(idLugar).append("\n");
        mensaje.append("Estado: ").append(estado).append("\n");
        mensaje.append("GeoJSON: ").append(geojson).append("\n");
        mensaje.append("Color: ").append(color).append("\n");

        new AlertDialog.Builder(context)
                .setTitle("Información: " + nombre)
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void mostrarInfoEspacio(JsonObject data) {
        int idEspacio = data.get("id_espacio").getAsInt();
        int idLugar = data.get("id_lugar").getAsInt();
        int idPiso = data.get("id_piso").getAsInt();

        String nombre = data.get("nombre").getAsString();
        String descripcion = data.get("descripcion").getAsString();
        String url_imagenes = data.get("url_imagenes").getAsString();
        int estado = data.get("estado").getAsInt();
        String vertices = data.get("vertices").getAsString();

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("INFORMACIÓN DEL ESPACIO\n\n");
        mensaje.append("Nombre: ").append(nombre).append("\n");
        mensaje.append("Descripción: ").append(descripcion).append("\n");
        mensaje.append("Imágenes: ").append(url_imagenes).append("\n");
        mensaje.append("ID: ").append(idEspacio).append("\n");
        mensaje.append("ID Lugar: ").append(idLugar).append("\n");
        mensaje.append("ID Piso: ").append(idPiso).append("\n");
        mensaje.append("Estado: ").append(estado).append("\n");
        mensaje.append("Vértices: ").append(vertices).append("\n");

        new AlertDialog.Builder(context)
                .setTitle("Información: " + nombre)
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    // ════════════════════════════════════════════════════════════════
    // GESTIÓN DE PISOS
    // ════════════════════════════════════════════════════════════════

    public void cargarPisos(int idLugar) {
        List<Pisos> pisos = db.getPisosByLugar(idLugar);
        List<Integer> listaPisosId = new ArrayList<>();
        List<String> nombresPisos = new ArrayList<>();

        Log.d("DEBUG_PISO", "=== CARGANDO PISOS ===");
        Log.d("DEBUG_PISO", "ID Lugar: " + idLugar);

        for (Pisos p : pisos) {
            Log.d("DEBUG_PISO", "Piso: ID=" + p.getId_piso() + " | Número=" + p.getNumero() + " | Nombre=" + p.getNombre());
            listaPisosId.add(p.getId_piso());
            nombresPisos.add(p.getNombre());
        }

        Log.d("DEBUG_PISO", "Nombres en spinner: " + nombresPisos.toString());
        Log.d("DEBUG_PISO", "IDs reales: " + listaPisosId.toString());

        if (nombresPisos.size() > 1) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    context,
                    android.R.layout.simple_spinner_item,
                    nombresPisos
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPisos.setAdapter(adapter);
            spinnerPisos.setTag(listaPisosId);
            spinnerPisos.setVisibility(View.VISIBLE);

            Toast.makeText(context, "Selecciona un piso", Toast.LENGTH_SHORT).show();

        } else if (nombresPisos.size() == 1) {
            spinnerPisos.setVisibility(View.GONE);
            mostrarEspaciosPorPiso(idLugar, listaPisosId.get(0));
            Toast.makeText(context, "Mostrando espacios (1 piso)", Toast.LENGTH_SHORT).show();

        } else {
            spinnerPisos.setVisibility(View.GONE);
            Toast.makeText(context, "No hay pisos registrados", Toast.LENGTH_SHORT).show();
        }
    }

    public void mostrarEspaciosPorPiso(int idLugar, int numeroPiso) {
        List<Espacio> espacios = db.getEspaciosByLugar(idLugar);

        Log.d("DEBUG_PISO", "=== DEBUGGING mostrarEspaciosPorPiso ===");
        Log.d("DEBUG_PISO", "ID Lugar: " + idLugar);
        Log.d("DEBUG_PISO", "Piso a buscar: " + numeroPiso);
        Log.d("DEBUG_PISO", "Total espacios en lugar: " + espacios.size());

        if (espacios.isEmpty()) {
            Toast.makeText(context, "No hay espacios", Toast.LENGTH_SHORT).show();
            Log.d("DEBUG_PISO", "Lista vacía");
            return;
        }

        int espaciosMostrados = 0;

        for (Espacio espacio : espacios) {
            Log.d("DEBUG_PISO",
                    "Espacio: " + espacio.getNombre() +
                            " | ID: " + espacio.getId_espacio() +
                            " | Piso: " + espacio.getId_piso());

            if (espacio.getId_piso() == numeroPiso) {
                Log.d("DEBUG_PISO", "✓ COINCIDE - Mostrando espacio");

                Geometria geo = db.getGeometriaByEspacio(espacio.getId_espacio());

                if (geo != null) {
                    dibujarPoligonoEspacio(geo, espacio);
                    espaciosMostrados++;
                }
            } else {
                Log.d("DEBUG_PISO", "✗ No coincide - Piso " + espacio.getId_piso() +
                        " != " + numeroPiso);
            }
        }

        Log.d("DEBUG_PISO", "Total mostrados: " + espaciosMostrados);

        if (espaciosMostrados == 0) {
            Toast.makeText(context, "No hay espacios en piso " + numeroPiso, Toast.LENGTH_SHORT).show();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // MODO EDICIÓN - AGREGAR PUNTOS Y CERRAR POLÍGONOS
    // ════════════════════════════════════════════════════════════════

    /**
     * Agregar un punto al modo de edición
     */
    public void agregarPunto(Point point, Style style) {
        puntosActuales.add(point);

        Bitmap bmp = crearPuntoBitmap("#ff0000");
        managerTemporal.create(
                new PointAnnotationOptions()
                        .withPoint(point)
                        .withIconImage(bmp)
                        .withIconSize(0.8)
        );

        if (puntosActuales.size() >= 2) {
            dibujarLineaPrevia(style);
        }
    }

    /**
     * Cerrar un lugar (polígono)
     */
    public void cerrarLugar(Style style) {
        if (puntosActuales.size() < 3) {
            Toast.makeText(context, "Mínimo 3 puntos", Toast.LENGTH_SHORT).show();
            return;
        }

        lugarActualId++;
        puntosActuales.add(puntosActuales.get(0));
        puntosLugarActual = new ArrayList<>(puntosActuales);

        String geojson = generarGeoJsonDesdePuntos(puntosActuales,
                "Lugar " + lugarActualId, "lugar", lugarActualId);

        mostrarGeoJsonEnDialogo("Lugar " + lugarActualId, geojson);
        logGeoJson("Lugar " + lugarActualId, geojson);

        dibujarPoligono(style, new ArrayList<>(puntosActuales),
                "lugar-" + lugarActualId, "#2196F3", 0.25);

        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Lugar " + lugarActualId, "#1565C0");

        Toast.makeText(context, "Lugar " + lugarActualId + " guardado",
                Toast.LENGTH_SHORT).show();

        puntosActuales.clear();
        managerTemporal.deleteAll();
    }

    /**
     * Cerrar un espacio (polígono)
     */
    public void cerrarEspacio(Style style) {
        if (puntosActuales.size() < 3) {
            Toast.makeText(context, "Mínimo 3 puntos", Toast.LENGTH_SHORT).show();
            return;
        }

        espacioContador++;
        puntosActuales.add(puntosActuales.get(0));

        String geojson = generarGeoJsonDesdePuntos(puntosActuales,
                "Espacio " + espacioContador, "espacio", espacioContador);

        mostrarGeoJsonEnDialogo("Espacio " + espacioContador, geojson);
        logGeoJson("Espacio " + espacioContador, geojson);

        String espacioId = "lugar-" + lugarActualId + "-espacio-" + espacioContador;
        dibujarPoligono(style, new ArrayList<>(puntosActuales),
                espacioId, "#FF9800", 0.4);

        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Espacio " + espacioContador, "#E65100");

        Toast.makeText(context, "Espacio " + espacioContador + " guardada",
                Toast.LENGTH_SHORT).show();

        puntosActuales.clear();
        managerTemporal.deleteAll();
    }

    /**
     * Deshacer último punto agregado
     */
    public void deshacerPunto() {
        if (!puntosActuales.isEmpty()) {
            puntosActuales.remove(puntosActuales.size() - 1);
            refrescarVertices();
        }
    }

    /**
     * Validar si un punto está dentro del lugar
     */
    public boolean puntoDentroDeLugar(Point punto) {
        if (puntosLugarActual.size() < 3) return false;
        double x = punto.longitude();
        double y = punto.latitude();
        boolean inside = false;
        int n = puntosLugarActual.size() - 1;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = puntosLugarActual.get(i).longitude();
            double yi = puntosLugarActual.get(i).latitude();
            double xj = puntosLugarActual.get(j).longitude();
            double yj = puntosLugarActual.get(j).latitude();
            if (((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Dibujar polígono en el mapa
     */
    public void dibujarPoligono(Style style, List<Point> puntos,
                                String id, String color, double opacidad) {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < puntos.size(); i++) {
            Point p = puntos.get(i);
            coords.append("[").append(p.longitude())
                    .append(",").append(p.latitude()).append("]");
            if (i < puntos.size() - 1) coords.append(",");
        }
        String geojson = "{\"type\":\"Feature\",\"geometry\":"
                + "{\"type\":\"Polygon\",\"coordinates\":[[" + coords
                + "]]},\"properties\":{}}";

        HashMap<String, Value> src = new HashMap<>();
        src.put("type", Value.valueOf("geojson"));
        src.put("data", Value.valueOf(geojson));
        style.addStyleSource(id + "-src", new Value(src));

        HashMap<String, Value> fillPaint = new HashMap<>();
        fillPaint.put("fill-color", Value.valueOf(color));
        fillPaint.put("fill-opacity", Value.valueOf(opacidad));
        HashMap<String, Value> fill = new HashMap<>();
        fill.put("id", Value.valueOf(id + "-fill"));
        fill.put("type", Value.valueOf("fill"));
        fill.put("source", Value.valueOf(id + "-src"));
        fill.put("paint", new Value(fillPaint));
        style.addStyleLayer(new Value(fill), null);

        HashMap<String, Value> linePaint = new HashMap<>();
        linePaint.put("line-color", Value.valueOf(color));
        linePaint.put("line-width", Value.valueOf(2));
        HashMap<String, Value> line = new HashMap<>();
        line.put("id", Value.valueOf(id + "-line"));
        line.put("type", Value.valueOf("line"));
        line.put("source", Value.valueOf(id + "-src"));
        line.put("paint", new Value(linePaint));
        style.addStyleLayer(new Value(line), null);
    }

    /**
     * Dibujar línea previa mientras se dibuja
     */
    private void dibujarLineaPrevia(Style style) {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < puntosActuales.size(); i++) {
            Point p = puntosActuales.get(i);
            coords.append("[").append(p.longitude())
                    .append(",").append(p.latitude()).append("]");
            if (i < puntosActuales.size() - 1) coords.append(",");
        }
        String geojson = "{\"type\":\"Feature\",\"geometry\":"
                + "{\"type\":\"LineString\",\"coordinates\":["
                + coords + "]},\"properties\":{}}";

        if (style.styleSourceExists("linea-preview")) {
            style.setStyleSourceProperty("linea-preview", "data", Value.valueOf(geojson));
        } else {
            HashMap<String, Value> src = new HashMap<>();
            src.put("type", Value.valueOf("geojson"));
            src.put("data", Value.valueOf(geojson));
            style.addStyleSource("linea-preview", new Value(src));

            HashMap<String, Value> paint = new HashMap<>();
            paint.put("line-color", Value.valueOf("#ff0000"));
            paint.put("line-width", Value.valueOf(2));
            HashMap<String, Value> layer = new HashMap<>();
            layer.put("id", Value.valueOf("linea-preview"));
            layer.put("type", Value.valueOf("line"));
            layer.put("source", Value.valueOf("linea-preview"));
            layer.put("paint", new Value(paint));
            style.addStyleLayer(new Value(layer), null);
        }
    }

    /**
     * Refrescar vértices temporales
     */
    private void refrescarVertices() {
        managerTemporal.deleteAll();
        for (Point p : puntosActuales) {
            Bitmap bmp = crearPuntoBitmap("#ff0000");
            managerTemporal.create(
                    new PointAnnotationOptions()
                            .withPoint(p)
                            .withIconImage(bmp)
                            .withIconSize(0.8)
            );
        }
    }

    /**
     * Agregar pin permanente
     */
    public void agregarPinPermanente(Point punto, String texto, String color) {
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
        managerPermanente.create(op);

        managerPermanente.addClickListener(annotation -> {
            Toast.makeText(context, "tocaste " + annotation.getTextField(),
                    Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    /**
     * Generar GeoJSON desde puntos
     */
    private String generarGeoJsonDesdePuntos(List<Point> puntos, String nombre,
                                             String tipo, int id) {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < puntos.size(); i++) {
            Point p = puntos.get(i);
            coords.append("[").append(p.longitude())
                    .append(",").append(p.latitude()).append("]");
            if (i < puntos.size() - 1) coords.append(",");
        }

        return "{\n" +
                "  \"type\": \"Feature\",\n" +
                "  \"properties\": {\n" +
                "    \"nombre\": \"" + nombre + "\",\n" +
                "    \"tipo\": \"" + tipo + "\",\n" +
                "    \"id\": " + id + ",\n" +
                "    \"timestamp\": " + System.currentTimeMillis() + "\n" +
                "  },\n" +
                "  \"geometry\": {\n" +
                "    \"type\": \"Polygon\",\n" +
                "    \"coordinates\": [[" + coords + "]]\n" +
                "  }\n" +
                "}";
    }

    /**
     * Mostrar GeoJSON en diálogo
     */
    private void mostrarGeoJsonEnDialogo(String titulo, String geojson) {
        String geojsonMostrar = geojson;
        if (geojson.length() > 800) {
            geojsonMostrar = geojson.substring(0, 800) + "\n\n... (truncado)";
        }

        new AlertDialog.Builder(context)
                .setTitle("📄 " + titulo)
                .setMessage(geojsonMostrar)
                .setPositiveButton("Copiar", (dialog, which) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager)
                                    context.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip =
                            android.content.ClipData.newPlainText("GeoJSON", geojson);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "GeoJSON copiado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    /**
     * Log GeoJSON en Logcat
     */
    private void logGeoJson(String titulo, String geojson) {
        Log.d("GEOJSON_DEBUG", "========== " + titulo + " ==========");
        Log.d("GEOJSON_DEBUG", geojson);
        Log.d("GEOJSON_DEBUG", "========== FIN ==========");
    }

    /**
     * Calcular centro desde puntos
     */
    private Point calcularCentro(List<Point> puntos) {
        double sumLng = 0, sumLat = 0;
        int total = puntos.size() - 1;
        for (int i = 0; i < total; i++) {
            sumLng += puntos.get(i).longitude();
            sumLat += puntos.get(i).latitude();
        }
        return Point.fromLngLat(sumLng / total, sumLat / total);
    }

    /**
     * Calcular centro desde GeoJSON
     */
    private Point calcularCentroDesdeGeoJson(String geojson) {
        try {
            int firstEnd = geojson.indexOf("]]]");
            if (firstEnd != -1) {
                geojson = geojson.substring(0, firstEnd + 3);
            }

            String clean = geojson
                    .replace("[[[", "")
                    .replace("]]]", "")
                    .trim();

            String[] puntosStr = clean.split("\\],\\[");

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

    // ════════════════════════════════════════════════════════════════
    // GETTERS PÚBLICOS
    // ════════════════════════════════════════════════════════════════

    public int obtenerCantidadPuntos() {
        return puntosActuales.size();
    }

    public void limpiarVérticesTemporales() {
        puntosActuales.clear();
        managerTemporal.deleteAll();
    }

    public int obtenerLugarActualId() {
        return lugarActualId;
    }

    public int obtenerEspacioContador() {
        return espacioContador;
    }

    public void resetearContadores() {
        lugarActualId = 0;
        espacioContador = 0;
    }
}