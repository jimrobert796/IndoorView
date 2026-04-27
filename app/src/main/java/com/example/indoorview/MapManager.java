package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Lugar;
import com.example.indoorview.models.Pisos;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.generated.FillLayer;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    public List<Point> puntosActuales = new ArrayList<>();
    public List<Point> puntosLugarActual = new ArrayList<>();
    public List<Point> puntosEspacioActual = new ArrayList<>();
    private int lugarActualId = 0;
    private int espacioContador = 0;

    // Array de imágenes para los 3 productos
    ImageView[] imgProductos = new ImageView[3];
    int imagenActual = 0;
    Intent tomarFotoIntento;

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_PICK_IMAGE = 2;

    private ActivityResultLauncher<Intent> launcherCamara;
    private ActivityResultLauncher<Intent> launcherGaleria;

    // Variables para imágenes
    private String urlFoto1 = "", urlFoto2 = "", urlFoto3 = "";

    // Variables temporales para el bottom sheet
    private JsonObject tempData;
    private boolean tempEsEspacio;
    private ImageView tempImageView;
    private boolean hayEspacios;

    // ════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════
    public MapManager(MapView mapView, Database db, Context context, Spinner spinner,ActivityResultLauncher<Intent> camaraLauncher,
                      ActivityResultLauncher<Intent> galeriaLauncher) {
        this.mapView = mapView;
        this.db = db;
        this.context = context;
        this.spinnerPisos = spinner;
        this.launcherCamara = camaraLauncher;
        this.launcherGaleria = galeriaLauncher;

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
            // Verifica si no hay pisos BUG SOLUCIONADO
            cargarPisos(lugarActualId);

            if (modoEdicion) {
                // Crud
                mostrarBottomSheetCRUD(data, false); // false = lugar
                Toast.makeText(context, "Listo para modificar CRUD", Toast.LENGTH_SHORT).show();
            } else {
                mostrarBottomSheetLugar(data);
                //mostrarInfoLugar(data);
                limpiarEspacios();
                lugarSeleccionado = -1;
                //Toast.makeText(context, "Espacios ocultos", Toast.LENGTH_SHORT).show();
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

                    limpiarEspacios();
                    limpiarEspaciosDeLugar(lugarSeleccionado);
                    cargarPisos(idLugar);

                    if(hayEspacios){
                        pinesOcultos.add(annotation);
                        annotation.setIconSize(0.0);
                        annotation.setTextOpacity(0.0);
                        managerLugares.update(annotation);
                        pinOcultoActual = annotation;
                    }

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
                // Espacios
                mostrarBottomSheetCRUD(data, true); // true = espacio
                Toast.makeText(context, "Listo para modificar CRUD", Toast.LENGTH_SHORT).show();
            } else {
                mostrarBottomSheetLugar(data);
                //mostrarInfoEspacio(data);
            }
            return true;
        });
    }

    // ════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS GENERALES
    // ════════════════════════════════════════════════════════════════


    // ════════════════════════════════════════════════════════════════
// MÉTODOS PARA BÚSQUEDA (Agregar estos a MapManager.java)
// ════════════════════════════════════════════════════════════════

    /**
     * Seleccionar un lugar desde la búsqueda
     * Simula un click en el pin del lugar
     */
    public void seleccionarLugarPorBusqueda(JsonObject data) {
        int idLugar = data.get("id_lugar").getAsInt();

        Log.d("SEARCH_SELECT", "Seleccionando lugar por búsqueda: ID " + idLugar);

        // Limpiar espacios anteriores
        limpiarEspacios();
        restaurarPinesOcultos();

        // Cargar pisos del lugar
        cargarPisos(lugarActualId);

        if (modoEdicion) {
            // En modo edición: mostrar CRUD
            mostrarBottomSheetCRUD(data, false); // false = lugar
            Toast.makeText(context, "Lugar seleccionado - Listo para modificar", Toast.LENGTH_SHORT).show();
        } else {
            // En modo vista: mostrar detalles
            mostrarBottomSheetLugar(data);
            limpiarEspacios();
            lugarSeleccionado = -1;
            Toast.makeText(context, "Lugar: " + data.get("nombre").getAsString(), Toast.LENGTH_SHORT).show();
        }

        // Animar cámara hacia el lugar
        if (data.has("geojson")) {
            Point centro = calcularCentroDesdeGeoJson(data.get("geojson").getAsString());
            if (centro != null) {
                redireccionPin(centro);
            }
        }
    }

    /**
     * Seleccionar un espacio desde la búsqueda
     * Simula un click en el pin del espacio
     */
    public void seleccionarEspacioPorBusqueda(JsonObject data) {
        int idEspacio = data.get("id_espacio").getAsInt();
        int idLugar = data.get("id_lugar").getAsInt();
        int idPiso = data.get("id_piso").getAsInt();
        String nombreEspacio = data.get("nombre").getAsString();

        Log.d("SEARCH_SELECT", "=========================================");
        Log.d("SEARCH_SELECT", "Seleccionando espacio por búsqueda:");
        Log.d("SEARCH_SELECT", "  Espacio: " + nombreEspacio + " (ID: " + idEspacio + ")");
        Log.d("SEARCH_SELECT", "  Lugar ID: " + idLugar);
        Log.d("SEARCH_SELECT", "  Piso ID: " + idPiso);
        Log.d("SEARCH_SELECT", "=========================================");

        // 1. LIMPIAR SELECCIÓN ANTERIOR
        limpiarEspacios();
        restaurarPinesOcultos();

        // 2. OCULTAR EL PIN DEL LUGAR
        ocultarPinLugar(idLugar);

        // 3. CARGAR Y SELECCIONAR EL PISO CORRECTO EN EL SPINNER
        lugarSeleccionado = idLugar;

        // Cargar todos los pisos del lugar
        List<Pisos> pisosDelLugar = db.getPisosByLugar(idLugar);

        // Encontrar el índice del piso que queremos seleccionar
        int pisoIndex = -1;
        int pisoIdSeleccionado = -1;
        for (int i = 0; i < pisosDelLugar.size(); i++) {
            Pisos piso = pisosDelLugar.get(i);
            if (piso.getId_piso() == idPiso) {
                pisoIndex = i;
                pisoIdSeleccionado = piso.getId_piso();
                Log.d("SEARCH_SELECT", "Piso encontrado: " + piso.getNombre() +
                        " (ID: " + piso.getId_piso() + ", #" + piso.getNumero() + ")");
                break;
            }
        }

        // 4. SELECCIONAR EL PISO EN EL SPINNER
        if (pisoIndex != -1 && spinnerPisos != null && pisosDelLugar.size() > 1) {
            // Mostrar y seleccionar el piso en el spinner
            mostrarPisosEnSpinner(pisosDelLugar, pisoIndex, idLugar);
        } else {
            // Si solo hay un piso o no hay spinner, mostrar directamente
            Log.d("SEARCH_SELECT", "Solo hay un piso o spinner no disponible, mostrando directamente");
        }

        // 5. MOSTRAR LOS ESPACIOS DEL PISO SELECCIONADO
        mostrarEspaciosPorPiso(idLugar, idPiso);

        // 6. ANIMAR CÁMARA HACIA EL ESPACIO
        if (data.has("vertices")) {
            Point centro = calcularCentroDesdeGeoJson(data.get("vertices").getAsString());
            if (centro != null) {
                new android.os.Handler().postDelayed(() -> {
                    redireccionPin(centro);
                }, 300); // Delay para que primero cargue los espacios
            }
        }

        // 7. MOSTRAR BOTTOM SHEET SEGÚN MODO
        if (modoEdicion) {
            mostrarBottomSheetCRUD(data, true);
            Toast.makeText(context, "Editando: " + nombreEspacio, Toast.LENGTH_SHORT).show();
        } else {
            mostrarBottomSheetLugar(data);

        }
    }

    // Metodo auxiliar para mostrar pisos en el spinner y seleccionar uno
    private void mostrarPisosEnSpinner(List<Pisos> pisos, int pisoSeleccionadoIndex, int idLugar) {
        if (spinnerPisos == null) {
            Log.e("SEARCH_SELECT", "Spinner es null");
            return;
        }

        // Mostrar spinner
        spinnerPisos.setVisibility(View.VISIBLE);

        // Crear lista de nombres de pisos
        List<String> nombresPisos = new ArrayList<>();
        List<Integer> idsPisos = new ArrayList<>();

        for (Pisos piso : pisos) {
            nombresPisos.add(piso.getNombre());
            idsPisos.add(piso.getId_piso());
        }

        // Crear adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                nombresPisos
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPisos.setAdapter(adapter);
        spinnerPisos.setTag(idsPisos);

        // Seleccionar el piso deseado
        spinnerPisos.setSelection(pisoSeleccionadoIndex);

        Log.d("SEARCH_SELECT", "Spinner configurado con " + pisos.size() + " pisos, seleccionado índice " + pisoSeleccionadoIndex);
    }
    public void ocultarPinLugar(int idLugar) {
        // Buscar el pin del lugar en el manager
        List<PointAnnotation> todosLosPines = managerLugares.getAnnotations();

        for (PointAnnotation pin : todosLosPines) {
            JsonObject data = (JsonObject) pin.getData();
            if (data != null && data.has("id_lugar")) {
                int pinIdLugar = data.get("id_lugar").getAsInt();
                if (pinIdLugar == idLugar) {
                    // Guardar referencia del pin que vamos a ocultar
                    pinOcultoActual = pin;
                    pinesOcultos.add(pin);

                    // Ocultar el pin (tamaño 0 y texto invisible)
                    pin.setIconSize(0.0);
                    pin.setTextOpacity(0.0);
                    managerLugares.update(pin);

                    Log.d("OCULTAR_PIN", "Pin del lugar " + idLugar + " ocultado");
                    break;
                }
            }
        }
    }

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
        int idGeo = geometria.getId_geometria();

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
                    agregarPinEspacio(centro, espacio.getNombre(), "#0080ff", espacio, geometria);
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

    private void agregarPinEspacio(Point punto, String texto, String color, Espacio espacio, Geometria geometria) {
        JsonObject data = new JsonObject();
        data.addProperty("id_espacio", espacio.getId_espacio());
        data.addProperty("id_geometria", geometria.getId_geometria());
        data.addProperty("id_lugar", espacio.getId_lugar());
        data.addProperty("id_piso", espacio.getId_piso());
        data.addProperty("nombre", espacio.getNombre());
        data.addProperty("descripcion", espacio.getDescripcion());
        data.addProperty("url_imagenes", espacio.getUrl_imagenes());
        data.addProperty("estado", espacio.getEstado());
        data.addProperty("vertices", geometria.getVertices());

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


    public void limpiarPinesTemporales(){
        managerPermanente.deleteAll();
    }


    public void limpiarGeometriaTemporalEspacios() {

        mapView.getMapboxMap().getStyle(style -> {
        if (style == null) {
            Log.e("LIMPIAR_GEO", "Style es null, abortando limpieza");
            return;
        }

        // 3️⃣ LIMPIAR POLÍGONO TEMPORAL DEL ESPACIO
        limpiarPoligonoTemporal(style, "espacio");

        Log.d("LIMPIAR_GEO", "════════════════════════════════════════════");
        Log.d("LIMPIAR_GEO", "✅ GEOMETRÍA TEMPORAL LIMPIADA COMPLETAMENTE");
        Log.d("LIMPIAR_GEO", "════════════════════════════════════════════");
    });


    };


    public void limpiarGeometriaTemporalCompleta() {
        Log.d("LIMPIAR_GEO", "════════════════════════════════════════════");
        Log.d("LIMPIAR_GEO", "LIMPIANDO GEOMETRÍA TEMPORAL COMPLETA");
        Log.d("LIMPIAR_GEO", "════════════════════════════════════════════");

        mapView.getMapboxMap().getStyle(style -> {
            if (style == null) {
                Log.e("LIMPIAR_GEO", "Style es null, abortando limpieza");
                return;
            }

            // 1️⃣ LIMPIAR LÍNEA DE PREVISUALIZACIÓN
            limpiarLineaPrevia(style);

            // 2️⃣ LIMPIAR POLÍGONO TEMPORAL DEL LUGAR
            limpiarPoligonoTemporal(style, "lugar");

            // 3️⃣ LIMPIAR POLÍGONO TEMPORAL DEL ESPACIO
            limpiarPoligonoTemporal(style, "espacio");

            // 4️⃣ LIMPIAR TODOS LOS PUNTOS TEMPORALES
            if (managerTemporal != null) {
                managerTemporal.deleteAll();
                Log.d("LIMPIAR_GEO", "✓ Puntos temporales eliminados");
            }

            // 5️⃣ LIMPIAR LISTAS DE PUNTOS
            if (puntosActuales != null) {
                puntosActuales.clear();
                Log.d("LIMPIAR_GEO", "✓ Lista puntosActuales limpiada");
            }

            if (puntosLugarActual != null) {
                puntosLugarActual.clear();
                Log.d("LIMPIAR_GEO", "✓ Lista puntosLugarActual limpiada");
            }

            if (puntosEspacioActual != null) {
                puntosEspacioActual.clear();
                Log.d("LIMPIAR_GEO", "✓ Lista puntosEspacioActual limpiada");
            }

            Log.d("LIMPIAR_GEO", "════════════════════════════════════════════");
            Log.d("LIMPIAR_GEO", "✅ GEOMETRÍA TEMPORAL LIMPIADA COMPLETAMENTE");
            Log.d("LIMPIAR_GEO", "════════════════════════════════════════════");
        });
    }

    /**
     * 🗑️ AUXILIAR: Limpiar línea de previsualización
     */
    private void limpiarLineaPrevia(Style style) {
        try {
            if (style.styleLayerExists("linea-preview")) {
                style.removeStyleLayer("linea-preview");
                Log.d("LIMPIAR_GEO", "✓ Capa 'linea-preview' eliminada");
            }

            if (style.styleSourceExists("linea-preview")) {
                style.removeStyleSource("linea-preview");
                Log.d("LIMPIAR_GEO", "✓ Fuente 'linea-preview' eliminada");
            }
        } catch (Exception e) {
            Log.e("LIMPIAR_GEO", "Error limpiando línea previa: " + e.getMessage());
        }
    }

    /**
     * 🗑️ AUXILIAR: Limpiar polígono temporal específico
     */
    private void limpiarPoligonoTemporal(Style style, String tipo) {
        try {
            Log.d("LIMPIAR_GEO", "Limpiando polígonos tipo: " + tipo);

            // Para lugares
            if (tipo.equals("lugar")) {
                for (int i = 0; i <= lugarActualId; i++) {
                    String baseId = "lugar-" + i;
                    limpiarCapasYFuentes(style, baseId);
                }
                Log.d("LIMPIAR_GEO", "✓ Polígonos de LUGAR limpiados (IDs: 0-" + lugarActualId + ")");
            }

            // Para espacios
            if (tipo.equals("espacio")) {
                for (int i = 0; i <= espacioContador; i++) {
                    String baseId = "lugar-" + lugarActualId + "-espacio-" + i;
                    limpiarCapasYFuentes(style, baseId);
                }
                Log.d("LIMPIAR_GEO", "✓ Polígonos de ESPACIO limpiados (IDs: 0-" + espacioContador + ")");
            }

        } catch (Exception e) {
            Log.e("LIMPIAR_GEO", "Error limpiando polígono " + tipo + ": " + e.getMessage());
        }
    }

    /**
     * 🗑️ AUXILIAR: Eliminar capas y fuentes por ID base
     */
    private void limpiarCapasYFuentes(Style style, String baseId) {
        try {
            // Eliminar capa fill
            if (style.styleLayerExists(baseId + "-fill")) {
                style.removeStyleLayer(baseId + "-fill");
                Log.d("LIMPIAR_GEO", "  ✓ Capa " + baseId + "-fill eliminada");
            }

            // Eliminar capa line
            if (style.styleLayerExists(baseId + "-line")) {
                style.removeStyleLayer(baseId + "-line");
                Log.d("LIMPIAR_GEO", "  ✓ Capa " + baseId + "-line eliminada");
            }

            // Eliminar fuente
            if (style.styleSourceExists(baseId + "-src")) {
                style.removeStyleSource(baseId + "-src");
                Log.d("LIMPIAR_GEO", "  ✓ Fuente " + baseId + "-src eliminada");
            }

        } catch (Exception e) {
            Log.e("LIMPIAR_GEO", "Error limpiando " + baseId + ": " + e.getMessage());
        }
    }

    /**
     * 🗑️ MEJORADO: Ahora limpia TODO (temporal y persistente) - Para reinicio completo
     */
    public void limpiarTodoTemporal() {
        Log.d("LIMPIAR_TOTAL", "════════════════════════════════════════════");
        Log.d("LIMPIAR_TOTAL", "LIMPIANDO TODO (TEMPORAL + PERSISTENTE)");
        Log.d("LIMPIAR_TOTAL", "════════════════════════════════════════════");

        // 1. Limpiar geometría temporal del mapa (líneas, polígonos)
        limpiarGeometriaTemporalCompleta();

        // 2. Limpiar puntos (ya se hace en limpiarGeometriaTemporalCompleta)
        if (managerTemporal != null) {
            managerTemporal.deleteAll();
        }

        // 3. Limpiar pins permanentes
        if (managerPermanente != null) {
            managerPermanente.deleteAll();
        }

        // 4. Limpiar URLs de imágenes
        urlFoto1 = "";
        urlFoto2 = "";
        urlFoto3 = "";
        Log.d("LIMPIAR_TOTAL", "✓ URLs de imágenes limpiadas");

        // 5. Resetear contadores (opcional)
        // resetearContadores();

        Log.d("LIMPIAR_TOTAL", "════════════════════════════════════════════");
        Log.d("LIMPIAR_TOTAL", "✅ TODO HA SIDO LIMPIADO");
        Log.d("LIMPIAR_TOTAL", "════════════════════════════════════════════");
    }
    /**
     * Limpia todos los elementos temporales creados durante el dibujo
     * Incluye: puntos temporales, líneas de previsualización, polígonos temporales
     */
    public void limpiarElementosTemporales() {
        // 1. Limpiar puntos temporales (los puntos rojos que se van agregando)
        if (managerTemporal != null) {
            managerTemporal.deleteAll();
        }

        // 2. Limpiar la lista de puntos actuales
        if (puntosActuales != null) {
            puntosActuales.clear();
        }

        // 3. Limpiar línea de previsualización del mapa
        mapView.getMapboxMap().getStyle(style -> {
            if (style.styleSourceExists("linea-preview")) {
                style.removeStyleSource("linea-preview");
            }
            if (style.styleLayerExists("linea-preview")) {
                style.removeStyleLayer("linea-preview");
            }
        });

        Log.d("LIMPIAR_TEMP", "Elementos temporales eliminados");
    }

    /**
     * Limpia elementos temporales y también elimina polígonos específicos por ID
     * @param idsPoligonos Lista de IDs de polígonos temporales a eliminar
     */
    public void limpiarPoligonosTemporales(List<String> idsPoligonos) {
        mapView.getMapboxMap().getStyle(style -> {
            for (String id : idsPoligonos) {
                // Eliminar source y layers del polígono temporal
                if (style.styleSourceExists(id + "-src")) {
                    style.removeStyleSource(id + "-src");
                }
                if (style.styleLayerExists(id + "-fill")) {
                    style.removeStyleLayer(id + "-fill");
                }
                if (style.styleLayerExists(id + "-line")) {
                    style.removeStyleLayer(id + "-line");
                }
            }
        });
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

    /**
     * NUEVO METODO - Mostrar diálogo para guardar un nuevo lugar
     */
    public void mostrarDialogoGuardarLugar(String geojson) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("💾 Guardar Nuevo LUGAR");

        // Crear vista personalizada con campos de entrada
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Campo: Nombre
        android.widget.TextView tvNombre = new android.widget.TextView(context);
        tvNombre.setText("Nombre del Lugar:");
        tvNombre.setTextSize(12);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvNombre);

        android.widget.EditText etNombre = new android.widget.EditText(context);
        etNombre.setHint("Ej: Edificio A");
        etNombre.setText("Lugar " + (lugarActualId));
        layout.addView(etNombre);

        // Campo: Descripción
        android.widget.TextView tvDescripcion = new android.widget.TextView(context);
        tvDescripcion.setText("Descripción:");
        tvDescripcion.setTextSize(12);
        tvDescripcion.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDescripcion.setPadding(0, 15, 0, 0);
        layout.addView(tvDescripcion);

        android.widget.EditText etDescripcion = new android.widget.EditText(context);
        etDescripcion.setHint("Describe este lugar");
        etDescripcion.setLines(3);
        layout.addView(etDescripcion);

        // Campo: URL Imágenes
        android.widget.TextView tvImagenes = new android.widget.TextView(context);
        tvImagenes.setText("URL de Imágenes:");
        tvImagenes.setTextSize(12);
        tvImagenes.setTypeface(null, android.graphics.Typeface.BOLD);
        tvImagenes.setPadding(0, 15, 0, 0);
        layout.addView(tvImagenes);

        android.widget.EditText etImagenes = new android.widget.EditText(context);
        etImagenes.setHint("https://ejemplo.com/imagen.jpg");
        layout.addView(etImagenes);

        // Campo: Color
        android.widget.TextView tvColor = new android.widget.TextView(context);
        tvColor.setText("Color (Hex):");
        tvColor.setTextSize(12);
        tvColor.setTypeface(null, android.graphics.Typeface.BOLD);
        tvColor.setPadding(0, 15, 0, 0);
        layout.addView(tvColor);

        android.widget.EditText etColor = new android.widget.EditText(context);
        etColor.setHint("#2196F3");
        etColor.setText("#2196F3");
        layout.addView(etColor);

        // Información del GeoJSON
        android.widget.TextView tvGeoJSON = new android.widget.TextView(context);
        tvGeoJSON.setText("GeoJSON (Automático):");
        tvGeoJSON.setTextSize(12);
        tvGeoJSON.setTypeface(null, android.graphics.Typeface.BOLD);
        tvGeoJSON.setPadding(0, 15, 0, 0);
        layout.addView(tvGeoJSON);

        android.widget.TextView tvGeoJSONValue = new android.widget.TextView(context);
        String geoJsonPreview = geojson.length() > 100
                ? geojson.substring(0, 100) + "..."
                : geojson;
        tvGeoJSONValue.setText(geoJsonPreview);
        tvGeoJSONValue.setTextSize(10);
        tvGeoJSONValue.setTextColor(Color.GRAY);
        layout.addView(tvGeoJSONValue);

        // Agregar scroll si es necesario
        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        scrollView.addView(layout);

        builder.setView(scrollView);

        // Botones
        builder.setPositiveButton("💾 Guardar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String imagenes = etImagenes.getText().toString().trim();
            String color = etColor.getText().toString().trim();

            // Validar que el nombre no esté vacío
            if (nombre.isEmpty()) {
                Toast.makeText(context, "El nombre es requerido", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log de datos a guardar
            Log.d("GUARDAR_LUGAR", "========== DATOS A GUARDAR ==========");
            Log.d("GUARDAR_LUGAR", "Nombre: " + nombre);
            Log.d("GUARDAR_LUGAR", "Descripción: " + descripcion);
            Log.d("GUARDAR_LUGAR", "Imágenes: " + imagenes);
            Log.d("GUARDAR_LUGAR", "Color: " + color);
            Log.d("GUARDAR_LUGAR", "GeoJSON: " + geojson);
            Log.d("GUARDAR_LUGAR", "=====================================");

            Toast.makeText(context, "✓ Lugar guardado: " + nombre, Toast.LENGTH_LONG).show();

            // TODO: Implementar guardado real en BD
            // db.insertLugar(nombre, descripcion, imagenes, color, geojson, 1);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            Toast.makeText(context, "Guardado cancelado", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    /**
     * NUEVO METODO CONCEPTO NO FINAL ES PARA DARSE LA IDEA DE COMO SE GAURDARIA EN BD AUNQUE NECESITP MANEJAR LOS PISOS
     */
    public void mostrarDialogoGuardarEspacio(String geojson) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("💾 Guardar Nuevo ESPACIO");

        // Crear vista personalizada con campos de entrada
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Campo: Nombre
        android.widget.TextView tvNombre = new android.widget.TextView(context);
        tvNombre.setText("Nombre del Espacio:");
        tvNombre.setTextSize(12);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvNombre);

        android.widget.EditText etNombre = new android.widget.EditText(context);
        etNombre.setHint("Ej: Aula 101");
        etNombre.setText("Espacio " + (espacioContador));
        layout.addView(etNombre);

        // Campo: Descripción
        android.widget.TextView tvDescripcion = new android.widget.TextView(context);
        tvDescripcion.setText("Descripción:");
        tvDescripcion.setTextSize(12);
        tvDescripcion.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDescripcion.setPadding(0, 15, 0, 0);
        layout.addView(tvDescripcion);

        android.widget.EditText etDescripcion = new android.widget.EditText(context);
        etDescripcion.setHint("Describe este espacio");
        etDescripcion.setLines(3);
        layout.addView(etDescripcion);

        // Campo: URL Imágenes
        android.widget.TextView tvImagenes = new android.widget.TextView(context);
        tvImagenes.setText("URL de Imágenes:");
        tvImagenes.setTextSize(12);
        tvImagenes.setTypeface(null, android.graphics.Typeface.BOLD);
        tvImagenes.setPadding(0, 15, 0, 0);
        layout.addView(tvImagenes);

        android.widget.EditText etImagenes = new android.widget.EditText(context);
        etImagenes.setHint("https://ejemplo.com/imagen.jpg");
        layout.addView(etImagenes);

        // Campo: ID Piso
        android.widget.TextView tvPiso = new android.widget.TextView(context);
        tvPiso.setText("ID del Piso:");
        tvPiso.setTextSize(12);
        tvPiso.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPiso.setPadding(0, 15, 0, 0);
        layout.addView(tvPiso);

        android.widget.EditText etPiso = new android.widget.EditText(context);
        etPiso.setHint("1");
        etPiso.setText("1");
        etPiso.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPiso);

        // Información del GeoJSON
        android.widget.TextView tvGeoJSON = new android.widget.TextView(context);
        tvGeoJSON.setText("GeoJSON (Automático):");
        tvGeoJSON.setTextSize(12);
        tvGeoJSON.setTypeface(null, android.graphics.Typeface.BOLD);
        tvGeoJSON.setPadding(0, 15, 0, 0);
        layout.addView(tvGeoJSON);

        android.widget.TextView tvGeoJSONValue = new android.widget.TextView(context);
        String geoJsonPreview = geojson.length() > 100
                ? geojson.substring(0, 100) + "..."
                : geojson;
        tvGeoJSONValue.setText(geoJsonPreview);
        tvGeoJSONValue.setTextSize(10);
        tvGeoJSONValue.setTextColor(Color.GRAY);
        layout.addView(tvGeoJSONValue);

        // Agregar scroll si es necesario
        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        scrollView.addView(layout);

        builder.setView(scrollView);

        // Botones
        builder.setPositiveButton("💾 Guardar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String imagenes = etImagenes.getText().toString().trim();
            String pisoPeek = etPiso.getText().toString().trim();

            // Validar que el nombre no esté vacío
            if (nombre.isEmpty()) {
                Toast.makeText(context, "El nombre es requerido", Toast.LENGTH_SHORT).show();
                return;
            }

            int idPiso = 1;
            try {
                idPiso = Integer.parseInt(pisoPeek);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "ID Piso inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log de datos a guardar
            Log.d("GUARDAR_ESPACIO", "========== DATOS A GUARDAR ==========");
            Log.d("GUARDAR_ESPACIO", "Nombre: " + nombre);
            Log.d("GUARDAR_ESPACIO", "Descripción: " + descripcion);
            Log.d("GUARDAR_ESPACIO", "Imágenes: " + imagenes);
            Log.d("GUARDAR_ESPACIO", "ID Piso: " + idPiso);
            Log.d("GUARDAR_ESPACIO", "GeoJSON: " + geojson);
            Log.d("GUARDAR_ESPACIO", "=====================================");

            Toast.makeText(context, "✓ Espacio guardado: " + nombre, Toast.LENGTH_LONG).show();

            // SE IMPLEMENTARA EN LA BASE DE DATOS
            // db.insertEspacio(lugarSeleccionado, idPiso, nombre, descripcion, url_imaganes, 1, color);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            Toast.makeText(context, "Guardado cancelado", Toast.LENGTH_SHORT).show();
        });

        builder.show();
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

        // Filtrar solo pisos que tienen espacios activos (estado = 1)
        for (Pisos p : pisos) {
            int cantidadEspaciosActivos = db.contarEspaciosActivosPorPiso(p.getId_piso());

            Log.d("DEBUG_PISO", "Piso: ID=" + p.getId_piso() +
                    " | Nombre=" + p.getNombre() +
                    " | Espacios activos: " + cantidadEspaciosActivos);


            // Solo agregar el piso si tiene al menos un espacio activo
            if (cantidadEspaciosActivos > 0) {
                listaPisosId.add(p.getId_piso());
                nombresPisos.add(p.getNombre());
            }
        }

        Log.d("DEBUG_PISO", "Pisos con espacios activos: " + nombresPisos.toString());
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
            hayEspacios = true;

            Toast.makeText(context, "Selecciona un piso", Toast.LENGTH_SHORT).show();

        } else if (nombresPisos.size() == 1) {
            spinnerPisos.setVisibility(View.GONE);
            mostrarEspaciosPorPiso(idLugar, listaPisosId.get(0));
            Toast.makeText(context, "Mostrando espacios (1 piso)", Toast.LENGTH_SHORT).show();

        } else {
            spinnerPisos.setVisibility(View.GONE);
            hayEspacios = false;
            //Toast.makeText(context, "No hay espacios disponibles en este lugar", Toast.LENGTH_SHORT).show();
        }
    }

    public void mostrarEspaciosPorPiso(int idLugar, int numeroPiso) {
        List<Espacio> espacios = db.getEspaciosByLugar(idLugar);

        Log.d("DEBUG_PISO", "=== DEBUGGING mostrarEspaciosPorPiso ===");
        Log.d("DEBUG_PISO", "ID Lugar: " + idLugar);
        Log.d("DEBUG_PISO", "Piso a buscar: " + numeroPiso);
        Log.d("DEBUG_PISO", "Total espacios en lugar: " + espacios.size());

        if (espacios.isEmpty()) {
            hayEspacios = false;
            Toast.makeText(context, "No hay espacios", Toast.LENGTH_SHORT).show();
            Log.d("DEBUG_PISO", "Lista vacía");
            return;
        }else {

            hayEspacios = true;
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
    // Modificar cerrarLugar(): MODIFICADO

    // Modificar cerrarLugar(): MODIFICADO
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

        // ✅ CAMBIADO: Usar el nuevo CRUD en modo creación
        mostrarCRUDCrearLugar(geojson);  // ← Esto muestra el BottomSheet CRUD

        dibujarPoligono(style, new ArrayList<>(puntosActuales),
                "lugar-" + lugarActualId, "#2196F3", 0.25);

        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Lugar " + lugarActualId, "#1565C0");

        puntosActuales.clear();
        managerTemporal.deleteAll();

        // ✅ Notificar al Fragment (opcional, el CRUD ya maneja el flujo)
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
        puntosEspacioActual = new ArrayList<>(puntosActuales);

        String geojson = generarGeoJsonDesdePuntos(puntosActuales,
                "Espacio " + espacioContador, "espacio", espacioContador);

        // ✅ CAMBIADO: Usar el nuevo CRUD en modo creación
        mostrarCRUDCrearEspacio(geojson);  // ← Esto muestra el BottomSheet CRUD

        String espacioId = "lugar-" + lugarActualId + "-espacio-" + espacioContador;
        dibujarPoligono(style, new ArrayList<>(puntosActuales),
                espacioId, "#FF9800", 0.4);

        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Espacio " + espacioContador, "#E65100");

        puntosActuales.clear();
        managerTemporal.deleteAll();

        // ✅ Notificar al Fragment (opcional)
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


    private void mostrarBottomSheetLugar(JsonObject data) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);

        View view = View.inflate(context, R.layout.bottom_sheet_detalle_lugar, null);
        dialog.setContentView(view);

        // Encontrar las vistas de imágenes y sus CardView
        ImageView imgFoto1 = view.findViewById(R.id.iv_foto1);
        ImageView imgFoto2 = view.findViewById(R.id.iv_foto2);
        ImageView imgFoto3 = view.findViewById(R.id.iv_foto3);

        // Encontrar los CardView
        androidx.cardview.widget.CardView cvFoto1 = view.findViewById(R.id.cv_foto1);
        androidx.cardview.widget.CardView cvFoto2 = view.findViewById(R.id.cv_foto2);
        androidx.cardview.widget.CardView cvFoto3 = view.findViewById(R.id.cv_foto3);

        // Extraer URLs válidas desde data
        String urlsImagenesStr = data.has("url_imagenes") ? data.get("url_imagenes").getAsString() : "";
        List<String> urlsValidas = new ArrayList<>();

        if (urlsImagenesStr != null && !urlsImagenesStr.isEmpty()) {
            String[] urls = urlsImagenesStr.split(",");
            for (String url : urls) {
                if (url != null && !url.isEmpty()) {
                    urlsValidas.add(url);
                }
            }
        }

        // Ocultar todos los CardView primero
        cvFoto1.setVisibility(View.GONE);
        cvFoto2.setVisibility(View.GONE);
        cvFoto3.setVisibility(View.GONE);

        // Mostrar solo las imágenes que existen
        if (urlsValidas.size() > 0) {
            cargarImagenEnImageView(imgFoto1, urlsValidas.get(0));
            cvFoto1.setVisibility(View.VISIBLE);

            final int pos0 = 0;
            imgFoto1.setOnClickListener(v -> {
                mostrarVisorImagenes(urlsValidas.toArray(new String[0]), pos0);
            });
        }

        if (urlsValidas.size() > 1) {
            cargarImagenEnImageView(imgFoto2, urlsValidas.get(1));
            cvFoto2.setVisibility(View.VISIBLE);

            final int pos1 = 1;
            imgFoto2.setOnClickListener(v -> {
                mostrarVisorImagenes(urlsValidas.toArray(new String[0]), pos1);
            });
        }

        if (urlsValidas.size() > 2) {
            cargarImagenEnImageView(imgFoto3, urlsValidas.get(2));
            cvFoto3.setVisibility(View.VISIBLE);

            final int pos2 = 2;
            imgFoto3.setOnClickListener(v -> {
                mostrarVisorImagenes(urlsValidas.toArray(new String[0]), pos2);
            });
        }

        // Ocultar el carrusel si no hay imágenes
        HorizontalScrollView hsvFotos = view.findViewById(R.id.hsv_fotos);
        if (urlsValidas.isEmpty()) {
            hsvFotos.setVisibility(View.GONE);
            Toast.makeText(context, "Este lugar no tiene imágenes", Toast.LENGTH_SHORT).show();
        } else {
            hsvFotos.setVisibility(View.VISIBLE);
        }

        // Referencias UI
        TextView tvTitulo = view.findViewById(R.id.tv_titulo_lugar);
        TextView tvDescripcion = view.findViewById(R.id.tv_descripcion);

        // Datos
        String nombre = data.get("nombre").getAsString();
        String descripcion = data.get("descripcion").getAsString();

        // Setear datos
        tvTitulo.setText(nombre);
        tvDescripcion.setText(descripcion);

        dialog.show();
    }


    // Método para extraer URLs del JSON
    private String[] extraerUrlsDeData(JsonObject data) {
        String[] urls = new String[3]; // Inicializar array para 3 imágenes
        try {
            String urlsImagenes = data.has("url_imagenes") ? data.get("url_imagenes").getAsString() : "";

            if (!urlsImagenes.isEmpty()) {
                String[] urlsArray = urlsImagenes.split(",");
                for (int i = 0; i < urlsArray.length && i < 3; i++) {
                    urls[i] = urlsArray[i];
                }
            }
        } catch (Exception e) {
            Log.e("VISOR", "Error extrayendo URLs: " + e.getMessage());
        }
        return urls;
    }

    // Visor de imágenes estilo Google Maps
    private void mostrarVisorImagenes(String[] urls, int posicionInicial) {
        // Filtrar solo URLs válidas
        List<String> urlsValidas = new ArrayList<>();
        for (String url : urls) {
            if (url != null && !url.isEmpty()) {
                urlsValidas.add(url);
            }
        }

        if (urlsValidas.isEmpty()) {
            Toast.makeText(context, "No hay imágenes para mostrar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear el diálogo a pantalla completa
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.FullScreenImageDialog );
        View view = View.inflate(context, R.layout.dialog_visor_imagenes, null);

        // Configurar el ViewPager para swipe entre imágenes
        androidx.viewpager.widget.ViewPager viewPager = view.findViewById(R.id.view_pager_imagenes);
        TextView tvContador = view.findViewById(R.id.tv_contador_imagenes);
        ImageView btnCerrar = view.findViewById(R.id.btn_cerrar_visor);
        TextView tvTituloImagen = view.findViewById(R.id.tv_titulo_imagen);

        // Crear adapter para el ViewPager
        ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(urlsValidas, context);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(posicionInicial);

        // Actualizar contador
        actualizarContador(posicionInicial, urlsValidas.size(), tvContador);

        // Listener para cambios de página
        viewPager.addOnPageChangeListener(new androidx.viewpager.widget.ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                actualizarContador(position, urlsValidas.size(), tvContador);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        // Configurar gestos de zoom (se maneja en el adapter)

        // Botón cerrar
        btnCerrar.setOnClickListener(v -> {
            AlertDialog dialog = (AlertDialog) view.getTag();
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        view.setTag(dialog);

        // Configurar para que ocupe toda la pantalla
        dialog.show();
        dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT
        );
    }

    private void actualizarContador(int posicion, int total, TextView tvContador) {
        tvContador.setText((posicion + 1) + " / " + total);
    }

    // Contiene la funcionalidad de crud
    // ════════════════════════════════════════════════════════════════
// VERSIÓN MEJORADA: mostrarBottomSheetCRUD() - 4 parámetros
// ════════════════════════════════════════════════════════════════

    /**
     * Versión MEJORADA que soporta tanto CREACIÓN como EDICIÓN
     * @param data JsonObject con datos
     * @param esEspacio true si es espacio, false si es lugar
     * @param modoCreacion true si estamos creando, false si editando existente
     * @param geojsonGuardado GeoJSON ya dibujado (para creación)
     */
    private void mostrarBottomSheetCRUD(JsonObject data, boolean esEspacio,
                                        boolean modoCreacion, String geojsonGuardado) {

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = View.inflate(context, R.layout.bottom_sheet_detalle_lugarespacio_crud, null);
        dialog.setContentView(view);

        // COLORES Y OPCIONES
        List<String> opcionesColor = Arrays.asList(
                "Azul",
                "Naranja",
                "Verde",
                "Amarillo",
                "Gris"
        );

        List<String> coloresHex = Arrays.asList(
                "#2196F3",
                "#ed5407",
                "#0fbf02",
                "#fce005",
                "#607D8B"
        );

        // UI
        EditText etNombre = view.findViewById(R.id.et_nombre);
        EditText etDescripcion = view.findViewById(R.id.et_descripcion);
        Button btnEditar = view.findViewById(R.id.btn_editar);
        Button btnGuardar = view.findViewById(R.id.btn_guardar);
        Button btnEliminar = view.findViewById(R.id.btn_eliminar);
        Spinner spinnerColor = view.findViewById(R.id.spinner_color);

        ImageView imgFoto1 = view.findViewById(R.id.iv_foto1);
        ImageView imgFoto2 = view.findViewById(R.id.iv_foto2);
        ImageView imgFoto3 = view.findViewById(R.id.iv_foto3);

        // ════════════════════════════════════════════════════════════════
        // AJUSTAR UI SEGÚN MODO
        // ════════════════════════════════════════════════════════════════
        if (modoCreacion) {
            // MODO CREACIÓN: todos los campos habilitados
            etNombre.setEnabled(true);
            etDescripcion.setEnabled(true);
            spinnerColor.setEnabled(true);

            btnEditar.setVisibility(View.GONE);     // Sin botón editar en creación
            btnGuardar.setVisibility(View.VISIBLE); // Guardar visible
            btnEliminar.setVisibility(View.GONE);   // Sin eliminar en creación
        } else {
            // MODO EDICIÓN: lógica original
            spinnerColor.setEnabled(false);
            btnEliminar.setVisibility(View.VISIBLE);
        }

        // Variable para controlar modo edición
        final boolean[] editando = {modoCreacion};

        // ════════════════════════════════════════════════════════════════
        // LISTENERS DE IMÁGENES
        // ════════════════════════════════════════════════════════════════
        imgFoto1.setOnClickListener(v -> {
            if (editando[0]) {
                mostrarOpcionesImagen(imgFoto1, 0, data, esEspacio);
            } else {
                String url = obtenerUrlFoto(0);
                if (url != null && !url.isEmpty()) {
                    mostrarMensaje("Toca el botón editar para cambiar la imagen");
                }
            }
        });

        imgFoto2.setOnClickListener(v -> {
            if (editando[0]) {
                mostrarOpcionesImagen(imgFoto2, 1, data, esEspacio);
            } else {
                String url = obtenerUrlFoto(1);
                if (url != null && !url.isEmpty()) {
                    mostrarMensaje("Toca el botón editar para cambiar la imagen");
                }
            }
        });

        imgFoto3.setOnClickListener(v -> {
            if (editando[0]) {
                mostrarOpcionesImagen(imgFoto3, 2, data, esEspacio);
            } else {
                String url = obtenerUrlFoto(2);
                if (url != null && !url.isEmpty()) {
                    mostrarMensaje("Toca el botón editar para cambiar la imagen");
                }
            }
        });

        cargarImagenesExistentes(data, imgFoto1, imgFoto2, imgFoto3);

        // DATOS ORIGINALES
        String nombreOriginal = data.get("nombre").getAsString();
        String descripcionOriginal = data.get("descripcion").getAsString();

        etNombre.setText(nombreOriginal);
        etDescripcion.setText(descripcionOriginal);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                opcionesColor
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColor.setAdapter(adapter);

        // ════════════════════════════════════════════════════════════════
        // LISTENER DEL SPINNER DE COLOR
        // ════════════════════════════════════════════════════════════════
        final boolean[] primeraVez = {true};

        spinnerColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (primeraVez[0]) {
                    primeraVez[0] = false;
                    return;
                }

                String colorSeleccionado = coloresHex.get(position);

                // En modo EDICIÓN: actualizar color en tiempo real
                if (!modoCreacion) {
                    actualizarColorLugar(
                            esEspacio ? data.get("id_espacio").getAsInt() : data.get("id_lugar").getAsInt(),
                            colorSeleccionado,
                            esEspacio
                    );
                }

                data.addProperty("color", colorSeleccionado);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String colorActual = data.has("color")
                ? data.get("color").getAsString()
                : "#2196F3";

        int indexSeleccionado = coloresHex.indexOf(colorActual);
        if (indexSeleccionado != -1) {
            spinnerColor.setSelection(indexSeleccionado);
        }

        // ════════════════════════════════════════════════════════════════
        // BOTÓN EDITAR (solo en modo edición)
        // ════════════════════════════════════════════════════════════════
        if (!modoCreacion) {
            btnEditar.setOnClickListener(v -> {
                if (!editando[0]) {
                    mostrarDialogoConfirmacion(
                            "Modo edición",
                            "¿Deseas editar este elemento?",
                            "Sí",
                            () -> {
                                editando[0] = true;
                                etNombre.setEnabled(true);
                                etDescripcion.setEnabled(true);
                                spinnerColor.setEnabled(true);
                                btnGuardar.setVisibility(View.VISIBLE);
                                btnEditar.setText("Cancelar");
                                Toast.makeText(context, "Modo edición activado", Toast.LENGTH_SHORT).show();
                            }
                    );
                } else {
                    mostrarDialogoConfirmacion(
                            "Cancelar edición",
                            "¿Deseas descartar los cambios?",
                            "Sí",
                            () -> {
                                editando[0] = false;
                                etNombre.setText(nombreOriginal);
                                etDescripcion.setText(descripcionOriginal);
                                etNombre.setEnabled(false);
                                etDescripcion.setEnabled(false);
                                spinnerColor.setEnabled(false);
                                btnGuardar.setVisibility(View.GONE);
                                btnEditar.setText("Editar");
                                Toast.makeText(context, "Edición cancelada", Toast.LENGTH_SHORT).show();
                            }
                    );
                }
            });
        }

        // ════════════════════════════════════════════════════════════════
        // BOTÓN GUARDAR
        // ════════════════════════════════════════════════════════════════
        btnGuardar.setOnClickListener(v -> {
            String colorSeleccionado = coloresHex.get(spinnerColor.getSelectedItemPosition());
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevaDesc = etDescripcion.getText().toString().trim();

            if (nuevoNombre.isEmpty()) {
                Toast.makeText(context, "El nombre es requerido", Toast.LENGTH_SHORT).show();
                return;
            }if (nuevaDesc.isEmpty()) {
                Toast.makeText(context, "La descripcion es requerido", Toast.LENGTH_SHORT).show();
                return;
            }

            String urlsImagenes = combinarUrlsImagenes();


            /*

            PODEMOS MEJORARLO Y OPTIMIZARLO MUCHISIMO MEJOOOOORRRRRRRRRRRR
             */

            String tipo = esEspacio ? "ESPACIO" : "LUGAR";

            if (modoCreacion) {
                // ✅ MODO CREACIÓN: Notificar al Fragment via callback
                Log.d("CRUD_CREAR", "Guardando " + (esEspacio ? "Espacio" : "Lugar"));
                Log.d("CRUD_CREAR", "Nombre: " + nuevoNombre);
                Log.d("CRUD_CREAR", "Color: " + colorSeleccionado);
                Log.d("CRUD_CREAR", "geometria: " + geojsonGuardado);

                mostrarDialogoConfirmacion("Guardar "+ tipo, "Estas seguro de guardar", "Si", () ->{
                    if (flujoCRUDListener != null) {
                        if (esEspacio) {
                            flujoCRUDListener.onEspacioGuardado(nuevoNombre, nuevaDesc, urlsImagenes, colorSeleccionado);
                        } else {
                            flujoCRUDListener.onLugarGuardado(nuevoNombre, nuevaDesc, urlsImagenes, colorSeleccionado);
                        }
                    }
                    Toast.makeText(context, "✓ " + (esEspacio ? "Espacio" : "Lugar") + " creado", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });


            } else {
                mostrarDialogoConfirmacion("Guardar "+ tipo, "Estas seguro de guardar", "Si", () ->{

                    // ✅ MODO EDICIÓN: Guardar en BD (lógica existente)
                    if (esEspacio) {
                        actualizarColorLugar(data.get("id_espacio").getAsInt(), colorSeleccionado, true);
                        data.addProperty("color", colorSeleccionado);
                        db.updateEspacio(
                                data.get("id_espacio").getAsInt(),
                                nuevoNombre,
                                nuevaDesc,
                                urlsImagenes
                        );
                        db.updateGeometriaColor(data.get("id_geometria").getAsInt(), colorSeleccionado);
                        Toast.makeText(context, "Espacio actualizado", Toast.LENGTH_SHORT).show();

                    } else {
                        actualizarColorLugar(data.get("id_lugar").getAsInt(), colorSeleccionado, false);
                        data.addProperty("color", colorSeleccionado);
                        db.updateLugar(
                                data.get("id_lugar").getAsInt(),
                                nuevoNombre,
                                nuevaDesc,
                                colorSeleccionado,
                                urlsImagenes
                        );
                        Toast.makeText(context, "Lugar actualizado", Toast.LENGTH_SHORT).show();
                        limpiarTodo();
                        cargarPoligonosLugar();
                    }
                    dialog.dismiss();
                });
            }
        });



        // ════════════════════════════════════════════════════════════════
        // BOTÓN ELIMINAR (solo en modo edición)
        // ════════════════════════════════════════════════════════════════
        if (!modoCreacion) {
            btnEliminar.setOnClickListener(v -> {
                mostrarDialogoConfirmacion(
                        "Confirmar eliminación",
                        "Esta acción no se puede deshacer, si elimina un lugar todos sus espacios se eliminaran en conjunto",
                        "Eliminar",
                        () -> {
                            if (esEspacio) {
                                // SOFT DELETE
                                int id = data.get("id_espacio").getAsInt();
                                db.eliminarEspacio(id);

                                // Re inciar el mapa
                                limpiarEspacios();
                                limpiarLugares();
                                limpiarTodo();
                                cargarPoligonosLugar();

                                Toast.makeText(context, "Espacio eliminado", Toast.LENGTH_SHORT).show();
                            } else {
                                // SOFT DELETE (en cadena ya que espacio depende de lugar)
                                int id = data.get("id_lugar").getAsInt();
                                db.eliminarLugar(id);



                                // Re inciar el mapa
                                limpiarEspacios();
                                limpiarLugares();
                                limpiarTodo();
                                cargarPoligonosLugar();
                                Toast.makeText(context, "Lugar eliminado", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        }
                );
            });
        }

        dialog.show();
    }

    private void mostrarBottomSheetCRUD(JsonObject data, boolean esEspacio) {
        mostrarBottomSheetCRUD(data, esEspacio, false, null);
    }

    public void mostrarOpcionesImagen(ImageView imageView, int indice, JsonObject data, boolean esEspacio) {
        // Guardar referencias para usar después
        this.tempImageView = imageView;
        this.imagenActual = indice;
        this.tempData = data;
        this.tempEsEspacio = esEspacio;

        String[] opciones = {"📷 Tomar foto con cámara", "🖼️ Seleccionar de galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Seleccionar imagen " + (indice + 1));
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                tomarFoto();
            } else {
                seleccionarDeGaleria();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    public void tomarFoto() {
        if (launcherCamara == null) {
            mostrarMensaje("Error: Launcher de cámara no inicializado");
            return;
        }

        Intent tomarFotoIntento = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File fotoProducto = null;

        try {
            fotoProducto = crearImgProducto();
            if (fotoProducto != null) {
                Uri uriFoto = FileProvider.getUriForFile(context,
                        "com.example.indoorview.fileprovider", fotoProducto);
                tomarFotoIntento.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto);
                launcherCamara.launch(tomarFotoIntento);
            } else {
                mostrarMensaje("No se pudo crear la foto");
            }
        } catch (Exception e) {
            mostrarMensaje("Error al tomar la foto: " + e.getMessage());
        }
    }

    public void seleccionarDeGaleria() {
        if (launcherGaleria == null) {
            mostrarMensaje("Error: Launcher de galería no inicializado");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        launcherGaleria.launch(intent);
    }

    public void procesarResultadoCamara(Intent data) {
        // Foto tomada con cámara
        String urlFoto = obtenerUrlFoto(imagenActual);
        if (!urlFoto.isEmpty() && tempImageView != null) {
            tempImageView.setImageURI(Uri.parse(urlFoto));
            tempImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mostrarMensaje("📷 Foto " + (imagenActual + 1) + " guardada");
        } else {
            mostrarMensaje("Error: No se pudo obtener la URL de la foto");
        }
    }

    public void procesarResultadoGaleria(Intent data) {
        // Imagen seleccionada de galería
        if (data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            String nuevaUrl = guardarImagenDesdeGaleria(selectedImageUri);
            if (!nuevaUrl.isEmpty()) {
                guardarUrlFoto(imagenActual, nuevaUrl);
                if (tempImageView != null) {
                    tempImageView.setImageURI(Uri.parse(nuevaUrl));
                    tempImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    mostrarMensaje("🖼️ Imagen " + (imagenActual + 1) + " seleccionada");
                }
            } else {
                mostrarMensaje("Error al guardar la imagen");
            }
        }
    }

    private File crearImgProducto() throws Exception {
        String fechaHoraMs = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "producto_" + fechaHoraMs + "_img" + imagenActual;

        File dirAlmacenamiento;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dirAlmacenamiento = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        } else {
            dirAlmacenamiento = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        }

        if (dirAlmacenamiento != null && !dirAlmacenamiento.exists()) {
            dirAlmacenamiento.mkdirs();
        }

        if (dirAlmacenamiento == null) {
            dirAlmacenamiento = context.getFilesDir();
        }

        File image = File.createTempFile(fileName, ".jpg", dirAlmacenamiento);
        guardarUrlFoto(imagenActual, image.getAbsolutePath());
        return image;
    }

    private String guardarImagenDesdeGaleria(Uri uri) {
        try {
            String fechaHoraMs = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "producto_galeria_" + fechaHoraMs + "_img" + imagenActual + ".jpg";

            File dirAlmacenamiento;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dirAlmacenamiento = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            } else {
                dirAlmacenamiento = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
            }

            if (dirAlmacenamiento != null && !dirAlmacenamiento.exists()) {
                dirAlmacenamiento.mkdirs();
            }

            if (dirAlmacenamiento == null) {
                dirAlmacenamiento = context.getFilesDir();
            }

            File imageFile = new File(dirAlmacenamiento, fileName);

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return imageFile.getAbsolutePath();

        } catch (Exception e) {
            mostrarMensaje("Error al guardar imagen: " + e.getMessage());
            return "";
        }
    }

    private String obtenerUrlFoto(int index) {
        switch (index) {
            case 0: return urlFoto1;
            case 1: return urlFoto2;
            case 2: return urlFoto3;
            default: return "";
        }
    }

    private void guardarUrlFoto(int index, String url) {
        switch (index) {
            case 0: urlFoto1 = url; break;
            case 1: urlFoto2 = url; break;
            case 2: urlFoto3 = url; break;
        }

        // Si tenemos datos temporales, actualizar también el JSON
        if (tempData != null) {
            String urlsActuales = tempData.has("url_imagenes") ? tempData.get("url_imagenes").getAsString() : "";
            String[] urls = urlsActuales.split(",");
            if (index < urls.length) {
                urls[index] = url;
            }
            String nuevasUrls = String.join(",", urls);
            tempData.addProperty("url_imagenes", nuevasUrls);
        }
    }

    // Método auxiliar para combinar las URLs de las imágenes
    private String combinarUrlsImagenes() {
        List<String> urlsValidas = new ArrayList<>();

        if (urlFoto1 != null && !urlFoto1.isEmpty()) {
            urlsValidas.add(urlFoto1);
        }
        if (urlFoto2 != null && !urlFoto2.isEmpty()) {
            urlsValidas.add(urlFoto2);
        }
        if (urlFoto3 != null && !urlFoto3.isEmpty()) {
            urlsValidas.add(urlFoto3);
        }

        String resultado = String.join(",", urlsValidas);
        Log.d("IMAGENES", "URLs combinadas: " + resultado);

        return resultado;
    }

    private void mostrarMensaje(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    private void cargarImagenesExistentes(JsonObject data, ImageView img1, ImageView img2, ImageView img3) {
        try {
            // Limpiar URLs actuales
            urlFoto1 = "";
            urlFoto2 = "";
            urlFoto3 = "";

            String urlsImagenes = data.has("url_imagenes") ? data.get("url_imagenes").getAsString() : "";

            if (urlsImagenes != null && !urlsImagenes.isEmpty()) {
                String[] urls = urlsImagenes.split(",");

                // Cargar imagen 1
                if (urls.length > 0 && urls[0] != null && !urls[0].isEmpty()) {
                    urlFoto1 = urls[0];
                    cargarImagenEnImageView(img1, urls[0]);
                } else {
                    img1.setImageResource(R.drawable.ic_placeholder);
                }

                // Cargar imagen 2
                if (urls.length > 1 && urls[1] != null && !urls[1].isEmpty()) {
                    urlFoto2 = urls[1];
                    cargarImagenEnImageView(img2, urls[1]);
                } else {
                    img2.setImageResource(R.drawable.ic_placeholder);
                }

                // Cargar imagen 3
                if (urls.length > 2 && urls[2] != null && !urls[2].isEmpty()) {
                    urlFoto3 = urls[2];
                    cargarImagenEnImageView(img3, urls[2]);
                } else {
                    img3.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                // Sin imágenes, mostrar placeholder
                img1.setImageResource(R.drawable.ic_placeholder);
                img2.setImageResource(R.drawable.ic_placeholder);
                img3.setImageResource(R.drawable.ic_placeholder);
            }
        } catch (Exception e) {
            Log.e("IMAGENES", "Error cargando imágenes: " + e.getMessage());
            img1.setImageResource(R.drawable.ic_placeholder);
            img2.setImageResource(R.drawable.ic_placeholder);
            img3.setImageResource(R.drawable.ic_placeholder);
        }
    }

    // Método auxiliar para cargar imagen desde URI
    private void cargarImagenEnImageView(ImageView imageView, String url) {
        try {
            if (url != null && !url.isEmpty()) {
                Uri uri = Uri.parse(url);
                imageView.setImageURI(uri);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView.setImageResource(R.drawable.ic_placeholder);
            }
        } catch (Exception e) {
            Log.e("IMAGENES", "Error cargando imagen en ImageView: " + e.getMessage());
            imageView.setImageResource(R.drawable.ic_placeholder);
        }
    }


    public void actualizarColorLugar(int id, String colorHex, boolean esEspacio) {
        mapView.getMapboxMap().getStyle(style -> {

            // Para espacios, el sourceId podría ser "espacio-" + id
            // Para lugares, "edificio-" + id
            String sourceId = esEspacio ? "espacio-" + id : "edificio-" + id;
            String fillLayerId = sourceId + "-fill";
            String lineLayerId = sourceId + "-line";

            try {
                // Cambiar color del relleno
                if (style.styleLayerExists(fillLayerId)) {
                    style.setStyleLayerProperty(
                            fillLayerId,
                            "fill-color",
                            Value.valueOf(colorHex)
                    );
                }

                // Cambiar color del borde
                if (style.styleLayerExists(lineLayerId)) {

                    String borderColor;

                    if (esEspacio) {
                        // Para ESPACIOS: borde NEGRO
                        borderColor = "#000000";
                    } else {
                        // Para LUGARES: borde del mismo color
                        borderColor = colorHex;
                    }

                    style.setStyleLayerProperty(
                            lineLayerId,
                            "line-color",
                            Value.valueOf(borderColor)
                    );
                }

                Log.d("MAPBOX_COLOR", "Color actualizado para " + (esEspacio ? "espacio" : "lugar") + " ID: " + id);

            } catch (Exception e) {
                Log.e("MAPBOX_COLOR", "Error: " + e.getMessage());
            }
        });
    }




    // DUNCION PERFECTA PARA CONFIRMACIONES
    public void mostrarDialogoConfirmacion(
            String titulo,
            String mensaje,
            String textoPositivo,
            Runnable onConfirm
    ) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton(textoPositivo, (d, w) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();

        // Cambiar color del botón positivo a #2196F3
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(Color.parseColor("#2196F3"));

        // Opcional: También cambiar el botón negativo si quieres
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setTextColor(Color.parseColor("#2196F3"));
    }

    // AHORA VAMOS A MEJORAR LO QUE SERIA AGREGAR SIN PROBLEMA ALGUNO HE?

    // En MapManager, agregar interface y variable:
    public interface OnLugarCerradoListener {
        void onLugarCerrado();
        void onEspacioCerrado();
    }

    private OnLugarCerradoListener flujoCerradoListener;


    public void setFlujoCerradoListener(OnLugarCerradoListener listener) {
        this.flujoCerradoListener = listener;
    }

    public interface OnFlujoCRUDListener {
        void onLugarGuardado(String nombre, String descripcion, String urlImagenes, String color);
        void onEspacioGuardado(String nombre, String descripcion, String urlImagenes, String color);
    }

    private OnFlujoCRUDListener flujoCRUDListener;

    public void setFlujoCRUDListener(OnFlujoCRUDListener listener) {
        this.flujoCRUDListener = listener;
    }

    /**
     * NUEVO: Mostrar CRUD en modo CREACIÓN para Lugar
     * Reutiliza el XML bottom_sheet_detalle_lugarespacio_crud
     */
    public void mostrarCRUDCrearLugar(String geojson) {
        JsonObject dataTemp = new JsonObject();
        dataTemp.addProperty("nombre", "Lugar " + (lugarActualId));
        dataTemp.addProperty("descripcion", "");
        dataTemp.addProperty("url_imagenes", "");
        dataTemp.addProperty("color", "#2196F3");
        dataTemp.addProperty("geojson", geojson);

        mostrarBottomSheetCRUD(dataTemp, false, true, geojson);
    }
    /**
     * NUEVO: Mostrar CRUD en modo CREACIÓN para Espacio
     * Reutiliza el XML bottom_sheet_detalle_lugarespacio_crud
     */
    public void mostrarCRUDCrearEspacio(String geojson) {
        JsonObject dataTemp = new JsonObject();
        dataTemp.addProperty("nombre", "Espacio " + espacioContador);
        dataTemp.addProperty("descripcion", "");
        dataTemp.addProperty("url_imagenes", "");
        dataTemp.addProperty("color", "#FF9800");
        dataTemp.addProperty("geojson", geojson);

        mostrarBottomSheetCRUD(dataTemp, true, true, geojson);
    }

    // METODOS PARA MANEJO DE ZOOM

    public void actualizarVisibilidadPinesPorZoom(double zoomActual) {
        // Configurar umbrales de zoom (puedes ajustar estos valores)
        double zoomMinimoLugares = 17.0;  // Mostrar lugares a partir de zoom 17
        double zoomMinimoPines = 18.5;    // Mostrar pines de espacios a partir de zoom 18.5

        // Actualizar visibilidad de pines de LUGARES
        if (zoomActual >= zoomMinimoLugares) {
            if (managerLugares.getAnnotations().isEmpty()) {
                Log.d("ZOOM_PINS", "✓ Mostrando pines de LUGARES (zoom: " + zoomActual + ")");
            }
            mostrarPinesLugaresZoom();
        } else {
            Log.d("ZOOM_PINS", "✗ Ocultando pines de LUGARES (zoom: " + zoomActual + ")");
            ocultarPinesLugaresZoom();
        }

        // Actualizar visibilidad de pines de ESPACIOS
        if (zoomActual >= zoomMinimoPines && managerEspacios.getAnnotations().size() > 0) {
            Log.d("ZOOM_PINS", "✓ Mostrando pines de ESPACIOS (zoom: " + zoomActual + ")");
            mostrarPinesEspaciosZoom();
        } else if (zoomActual < zoomMinimoPines) {
            Log.d("ZOOM_PINS", "✗ Ocultando pines de ESPACIOS (zoom: " + zoomActual + ")");
            ocultarPinesEspaciosZoom();
        }
    }

    private void mostrarPinesLugaresZoom() {
        try {
            List<PointAnnotation> pines = managerLugares.getAnnotations();
            for (PointAnnotation pin : pines) {
                // Solo mostrar si no está oculto por selección
                if (!pinesOcultos.contains(pin)) {
                    pin.setIconSize(0.9);
                    pin.setTextOpacity(1.0);
                    managerLugares.update(pin);
                }
            }
        } catch (Exception e) {
            Log.e("ZOOM_PINS", "Error mostrando pines de lugares: " + e.getMessage());
        }
    }

    /**
     * Ocultar todos los pines de lugares
     */
    private void ocultarPinesLugaresZoom() {
        try {
            List<PointAnnotation> pines = managerLugares.getAnnotations();
            for (PointAnnotation pin : pines) {
                pin.setIconSize(0.0);
                pin.setTextOpacity(0.0);
                managerLugares.update(pin);
            }
        } catch (Exception e) {
            Log.e("ZOOM_PINS", "Error ocultando pines de lugares: " + e.getMessage());
        }
    }

    /**
     * Mostrar todos los pines de espacios
     */
    private void mostrarPinesEspaciosZoom() {
        try {
            List<PointAnnotation> pines = managerEspacios.getAnnotations();
            for (PointAnnotation pin : pines) {
                pin.setIconSize(0.9);
                pin.setTextOpacity(1.0);
                managerEspacios.update(pin);
            }
        } catch (Exception e) {
            Log.e("ZOOM_PINS", "Error mostrando pines de espacios: " + e.getMessage());
        }
    }

    /**
     * Ocultar todos los pines de espacios
     */
    private void ocultarPinesEspaciosZoom() {
        try {
            List<PointAnnotation> pines = managerEspacios.getAnnotations();
            for (PointAnnotation pin : pines) {
                pin.setIconSize(0.0);
                pin.setTextOpacity(0.0);
                managerEspacios.update(pin);
            }
        } catch (Exception e) {
            Log.e("ZOOM_PINS", "Error ocultando pines de espacios: " + e.getMessage());
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





































