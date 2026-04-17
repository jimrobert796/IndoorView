package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Lugar;
import com.example.indoorview.models.Pisos;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.maps.CameraBoundsOptions;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.CoordinateBounds;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.generated.FillLayer;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;

    // Dos managers separados:
    // - uno para pines permanentes (lugars, espacios, UGB)
    // - uno para vértices temporales mientras dibuja
    private PointAnnotationManager managerPermanente;     // pruebas
    private PointAnnotationManager managerTemporal;

    // Modos
    private static final int MODO_NINGUNO = 0;
    private static final int MODO_LUGAR = 1;
    private static final int MODO_ESPACIO = 2;
    private int modoActual = MODO_NINGUNO;

    // Datos
    private List<Point> puntosActuales = new ArrayList<>();
    private List<Point> puntosLugarActual = new ArrayList<>();
    private int lugarActualId = 0;
    private int espacioContador = 0;

    // UI
    private Button btnLugar, btnEspacios, btnCerrar, btnDeshacer, btnFinalizar, btnHabilitar;
    private TextView tvModo;

    private boolean modoEdicionActivo = false;

    // Database
    private Database db;

    private MapManager mapManager;

    // Selecion de edificios
    private int lugarSeleccionadoId = -1;
    private boolean espaciosVisibles = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Inicalizaciones de variables XML
        mapView = findViewById(R.id.mapView);
        btnLugar = findViewById(R.id.btnEditar);
        btnEspacios = findViewById(R.id.btnEspacios);
        btnCerrar = findViewById(R.id.btnCerrar);
        btnDeshacer = findViewById(R.id.btnDeshacer);
        btnFinalizar = findViewById(R.id.btnFinalizar);
        tvModo = findViewById(R.id.tvModo);
        btnHabilitar = findViewById(R.id.btnHabilitar);


        // Inicializar base de datos
        db = Database.getInstance(this);

        // Inicializar MapManager
        mapManager = new MapManager(mapView, db, this);

        // Verificar conexión a la BD (opcional)
        mapManager.verificarConexionBD();


        // ================= CARGA DE MAPA =========================
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {

            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                            .zoom(17.0)
                            .build()
            );
            // ============ CONFIGURACIONES DE MAPA ==========

            // zoom min o max
            mapView.getMapboxMap().setBounds(
                    new CameraBoundsOptions.Builder()
                            .minZoom(16.0)
                            .maxZoom(20.0)
                            .build()
            );

            // Limitar área del mapa (bounding box)
            CoordinateBounds bounds = new CoordinateBounds(
                    Point.fromLngLat(-88.4195, 13.3435), // noreste (arriba derecha)
                    Point.fromLngLat(-88.4165, 13.3410));  // suroeste (abajo izquierda)

                    // Desactivar los edificios brindados por OpenStreetMap
            style.setStyleLayerProperty(
                    "building",
                    "visibility",
                    Value.valueOf("none")
            );

            style.setStyleLayerProperty(
                    "building-extrusion",
                    "visibility",
                    Value.valueOf("none")
            );


            //  Cargar los polígonos de los edificios
            mapManager.cargarPoligonosLugar();

            // Cargar los polígonos de los espacios (aulas)
            // mapManager.dibujarTodosLosEspacios();


            // Crear dos managers separados
            AnnotationPlugin annotationPlugin = mapView.getPlugin(Plugin.Mapbox.MAPBOX_ANNOTATION_PLUGIN_ID);

            managerPermanente = (PointAnnotationManager) annotationPlugin
                    .createAnnotationManager(AnnotationType.PointAnnotation, null);

            managerTemporal = (PointAnnotationManager) annotationPlugin
                    .createAnnotationManager(AnnotationType.PointAnnotation, null);


            // Pin UGB permanente
            agregarPinPermanente(
                    Point.fromLngLat(-88.41783453298294, 13.342296805328829),
                    "UGB", "#0080ff"
            );

            // Click en el mapa
            GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
                if (modoActual == MODO_LUGAR) {
                    agregarPunto(point, style);
                    return true;
                } else if (modoActual == MODO_ESPACIO) {
                    if (puntoDentroDeLugar(point)) {
                        agregarPunto(point, style);
                    } else {
                        Toast.makeText(this,
                                "El punto está fuera del lugar",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });
        });


        // Habilitacion
        // Ocultar todos los botones de edición por defaul
        ocultarEdicion();
        ;

        btnHabilitar.setOnClickListener(v -> {
            if (modoEdicionActivo) {
                // Desactivar modo edición - ocultar
                modoEdicionActivo = false;
                btnHabilitar.setText("HABILITAR");

                // Ocultar todos los botones de edición
                btnLugar.setVisibility(View.GONE);
                btnEspacios.setVisibility(View.GONE);
                btnCerrar.setVisibility(View.GONE);
                btnDeshacer.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.GONE);

                // Salir de cualquier modo activo
                if (modoActual != MODO_NINGUNO) {
                    modoActual = MODO_NINGUNO;
                    btnLugar.setText("Lugar");
                    puntosActuales.clear();
                    refrescarVertices();
                }

                tvModo.setText("");
                Toast.makeText(this, "Modo edición desactivado", Toast.LENGTH_SHORT).show();

            } else {
                // Activar modo edición - mostrar botones
                modoEdicionActivo = true;
                btnHabilitar.setText("CANCELAR");

                // Mostrar solo el botón Lugar inicialmente
                btnLugar.setVisibility(View.VISIBLE);
                btnEspacios.setVisibility(View.GONE);
                btnCerrar.setVisibility(View.GONE);
                btnDeshacer.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.GONE);

                tvModo.setText("Modo edición activado - Toca 'Lugar' para comenzar");
                Toast.makeText(this, "Modo edición activado", Toast.LENGTH_SHORT).show();
            }
        });


        // ── Botón Lugar ──
        btnLugar.setOnClickListener(v -> {
            if (modoActual == MODO_LUGAR) {
                modoActual = MODO_NINGUNO;
                btnLugar.setText("Lugar");
                ocultarBotonesEdicion();
                puntosActuales.clear();
                refrescarVertices();
                tvModo.setText("");
            } else {
                modoActual = MODO_LUGAR;
                btnLugar.setText("Cancelar");
                btnEspacios.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.GONE);
                btnCerrar.setVisibility(View.VISIBLE);
                btnDeshacer.setVisibility(View.VISIBLE);
                puntosActuales.clear();
                tvModo.setText("Dibuja el LUGAR — toca el mapa punto a punto");
            }
        });

        // ── Botón Espacios ──
        btnEspacios.setOnClickListener(v -> {
            if (modoActual == MODO_ESPACIO) {
                modoActual = MODO_NINGUNO;
                btnEspacios.setText("Espacios");
                btnCerrar.setVisibility(View.GONE);
                btnDeshacer.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.VISIBLE);
                puntosActuales.clear();
                refrescarVertices();
                tvModo.setText("Modo espacios terminado");
            } else {
                modoActual = MODO_ESPACIO;
                btnEspacios.setText("Salir espacios");
                btnCerrar.setVisibility(View.VISIBLE);
                btnDeshacer.setVisibility(View.VISIBLE);
                btnFinalizar.setVisibility(View.GONE);
                espacioContador = 0;
                puntosActuales.clear();
                tvModo.setText("Dibuja ESPACIOS dentro del Lugar " + lugarActualId);
            }
        });

        // ── Botón Cerrar polígono ──
        btnCerrar.setOnClickListener(v -> {
            if (puntosActuales.size() < 3) {
                Toast.makeText(this, "Mínimo 3 puntos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (modoActual == MODO_LUGAR) cerrarLugar();
            else if (modoActual == MODO_ESPACIO) cerrarEspacio();
        });

        // ── Botón Deshacer ──
        btnDeshacer.setOnClickListener(v -> {
            if (!puntosActuales.isEmpty()) {
                puntosActuales.remove(puntosActuales.size() - 1);
                refrescarVertices();
                tvModo.setText("Puntos: " + puntosActuales.size());
            }
        });

        // ── Botón Finalizar ──
        btnFinalizar.setOnClickListener(v -> {
            modoActual = MODO_NINGUNO;
            ocultarBotonesEdicion();
            btnLugar.setText("Lugar");
            btnFinalizar.setVisibility(View.GONE);
            btnEspacios.setVisibility(View.GONE);
            puntosActuales.clear();
            // Solo borramos vértices temporales, los pines permanentes quedan
            managerTemporal.deleteAll();
            tvModo.setText("¡Listo! Lugar y espacios guardados.");
            Toast.makeText(this, "Mapa guardado correctamente", Toast.LENGTH_LONG).show();
        });
    }

    // Oculta los botones para edicion de mapa
    private void ocultarEdicion(){
        btnLugar.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);
    }

    // Agregar punto temporal
    private void agregarPunto(Point point, Style style) {
        puntosActuales.add(point);
        // Vértice en manager temporal
        Bitmap bmp = crearPuntoBitmap("#ff0000"); // Naranja
        managerTemporal.create(
                new PointAnnotationOptions()
                        .withPoint(point)
                        .withIconImage(bmp)
                        .withIconSize(0.8)
        );
        tvModo.setText("Puntos: " + puntosActuales.size() + " — toca Cerrar para terminar");
        if (puntosActuales.size() >= 2) dibujarLineaPrevia(style);
    }

    // Cerrar LUGAR
    private void cerrarLugar() {
        lugarActualId++;
        puntosActuales.add(puntosActuales.get(0));
        puntosLugarActual = new ArrayList<>(puntosActuales);

        // GENERAR GEOJSON
        String geojson = generarGeoJsonDesdePuntos(puntosActuales,
                "Lugar " + lugarActualId, "lugar", lugarActualId);

        //MOSTRAR EN PANTALLA (DEBUG)
        mostrarGeoJsonEnDialogo("Lugar " + lugarActualId, geojson);
        logGeoJson("Lugar " + lugarActualId, geojson);  // También en Logcat


        mapView.getMapboxMap().getStyle(style ->
                dibujarPoligono(style, new ArrayList<>(puntosActuales),
                        "lugar-" + lugarActualId, "#2196F3", 0.25)
        );

        // Pin permanente en el centro
        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Lugar " + lugarActualId, "#1565C0");

        Toast.makeText(this, "Lugar " + lugarActualId + " guardado",
                Toast.LENGTH_SHORT).show();

        puntosActuales.clear();
        managerTemporal.deleteAll(); // ← solo borra vértices temporales
        modoActual = MODO_NINGUNO;
        btnLugar.setText("Lugar");
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.VISIBLE);
        btnFinalizar.setVisibility(View.VISIBLE);
        tvModo.setText("Lugar guardado. Dibuja espacios o finaliza.");
    }

    // ─────────────────────────────────────────
    // Cerrar ESPACIO
    // ─────────────────────────────────────────
    private void cerrarEspacio() {
        espacioContador++;
        puntosActuales.add(puntosActuales.get(0));

        // GENERAR GEOJSON
        String geojson = generarGeoJsonDesdePuntos(puntosActuales,
                "Espacio " + espacioContador, "espacio", espacioContador);

        // MOSTRAR EN PANTALLA (DEBUG)
        mostrarGeoJsonEnDialogo("Espacio " + espacioContador, geojson);
        logGeoJson("Espacio " + espacioContador, geojson);

        String espacioId = "lugar-" + lugarActualId + "-espacio-" + espacioContador;
        mapView.getMapboxMap().getStyle(style ->
                dibujarPoligono(style, new ArrayList<>(puntosActuales),
                        espacioId, "#FF9800", 0.4)
        );

        // Pin permanente en el centro
        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Espacio " + espacioContador, "#E65100");

        Toast.makeText(this, "Espacio " + espacioContador + " guardada",
                Toast.LENGTH_SHORT).show();

        puntosActuales.clear();
        managerTemporal.deleteAll(); // ← solo borra vértices temporales
        tvModo.setText("Espacio " + espacioContador + " guardada. Dibuja la siguiente.");
    }


    // ================ Pin permanente — usa managerPermanente ==================
    private void agregarPinPermanente(Point punto, String texto, String color) {
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
            Toast.makeText(this, "tocaste" + annotation.getTextField(),
                    Toast.LENGTH_SHORT).show();
            return true;
        });
    }


    // Validar punto dentro del lugar
    private boolean puntoDentroDeLugar(Point punto) {
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


    // Calcular centro para pin
    private Point calcularCentro(List<Point> puntos) {
        double sumLng = 0, sumLat = 0;
        int total = puntos.size() - 1;
        for (int i = 0; i < total; i++) {
            sumLng += puntos.get(i).longitude();
            sumLat += puntos.get(i).latitude();
        }
        return Point.fromLngLat(sumLng / total, sumLat / total);
    }

    // Dibujar polígono
    private void dibujarPoligono(Style style, List<Point> puntos,
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

    // Línea previa mientras dibuja
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

    // Refrescar solo vértices temporales
    private void refrescarVertices() {
        managerTemporal.deleteAll(); // ← solo borra temporales
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

    private void ocultarBotonesEdicion() {
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
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


    // Mostrar Geo.json en Dialogo para debug
    private void mostrarGeoJsonEnDialogo(String titulo, String geojson) {
        // Limitar longitud para no saturar el diálogo
        String geojsonMostrar = geojson;
        if (geojson.length() > 800) {
            geojsonMostrar = geojson.substring(0, 800) + "\n\n... (truncado)";
        }

        new AlertDialog.Builder(this)
                .setTitle("📄 " + titulo)
                .setMessage(geojsonMostrar)
                .setPositiveButton("Copiar", (dialog, which) -> {
                    // Copiar al portapapeles
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("GeoJSON", geojson);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "GeoJSON copiado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }
    // para el log
    private void logGeoJson(String titulo, String geojson) {
        Log.d("GEOJSON_DEBUG", "========== " + titulo + " ==========");
        Log.d("GEOJSON_DEBUG", geojson);
        Log.d("GEOJSON_DEBUG", "========== FIN ==========");
    }
    // para el log desde puntos
    private String generarGeoJsonDesdePuntos(List<Point> puntos, String nombre, String tipo, int id) {
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

    /*
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
     */
}