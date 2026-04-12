package com.example.indoorview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
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
    // - uno para pines permanentes (edificios, aulas, UGB)
    // - uno para vértices temporales mientras dibuja
    private PointAnnotationManager managerPermanente;
    private PointAnnotationManager managerTemporal;

    // Modos
    private static final int MODO_NINGUNO  = 0;
    private static final int MODO_EDIFICIO = 1;
    private static final int MODO_AULA     = 2;
    private int modoActual = MODO_NINGUNO;

    // Datos
    private List<Point> puntosActuales       = new ArrayList<>();
    private List<Point> puntosEdificioActual = new ArrayList<>();
    private int edificioActualId = 0;
    private int aulaContador     = 0;

    // UI
    private Button btnEdificio, btnAulas, btnCerrar, btnDeshacer, btnFinalizar;
    private TextView tvModo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView      = findViewById(R.id.mapView);
        btnEdificio  = findViewById(R.id.btnEditar);
        btnAulas     = findViewById(R.id.btnAulas);
        btnCerrar    = findViewById(R.id.btnCerrar);
        btnDeshacer  = findViewById(R.id.btnDeshacer);
        btnFinalizar = findViewById(R.id.btnFinalizar);
        tvModo       = findViewById(R.id.tvModo);

        // ================= CARGA DE MAPA =========================
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {

            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                            .zoom(17.0)
                            .build()
            );

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
                if (modoActual == MODO_EDIFICIO) {
                    agregarPunto(point, style);
                    return true;
                } else if (modoActual == MODO_AULA) {
                    if (puntoDentroDeEdificio(point)) {
                        agregarPunto(point, style);
                    } else {
                        Toast.makeText(this,
                                "El punto está fuera del edificio",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });
        });

        // ── Botón Edificio ──
        btnEdificio.setOnClickListener(v -> {
            if (modoActual == MODO_EDIFICIO) {
                modoActual = MODO_NINGUNO;
                btnEdificio.setText("Edificio");
                ocultarBotonesEdicion();
                puntosActuales.clear();
                refrescarVertices();
                tvModo.setText("");
            } else {
                modoActual = MODO_EDIFICIO;
                btnEdificio.setText("Cancelar");
                btnAulas.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.GONE);
                btnCerrar.setVisibility(View.VISIBLE);
                btnDeshacer.setVisibility(View.VISIBLE);
                puntosActuales.clear();
                tvModo.setText("Dibuja el EDIFICIO — toca el mapa punto a punto");
            }
        });

        // ── Botón Aulas ──
        btnAulas.setOnClickListener(v -> {
            if (modoActual == MODO_AULA) {
                modoActual = MODO_NINGUNO;
                btnAulas.setText("Aulas");
                btnCerrar.setVisibility(View.GONE);
                btnDeshacer.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.VISIBLE);
                puntosActuales.clear();
                refrescarVertices();
                tvModo.setText("Modo aulas terminado");
            } else {
                modoActual = MODO_AULA;
                btnAulas.setText("Salir aulas");
                btnCerrar.setVisibility(View.VISIBLE);
                btnDeshacer.setVisibility(View.VISIBLE);
                btnFinalizar.setVisibility(View.GONE);
                aulaContador = 0;
                puntosActuales.clear();
                tvModo.setText("Dibuja AULAS dentro del Edificio " + edificioActualId);
            }
        });

        // ── Botón Cerrar polígono ──
        btnCerrar.setOnClickListener(v -> {
            if (puntosActuales.size() < 3) {
                Toast.makeText(this, "Mínimo 3 puntos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (modoActual == MODO_EDIFICIO) cerrarEdificio();
            else if (modoActual == MODO_AULA) cerrarAula();
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
            btnEdificio.setText("Edificio");
            btnFinalizar.setVisibility(View.GONE);
            btnAulas.setVisibility(View.GONE);
            puntosActuales.clear();
            // Solo borramos vértices temporales, los pines permanentes quedan
            managerTemporal.deleteAll();
            tvModo.setText("¡Listo! Edificio y aulas guardados.");
            Toast.makeText(this, "Mapa guardado correctamente", Toast.LENGTH_LONG).show();
        });
    }

    // ─────────────────────────────────────────
    // Agregar punto temporal
    // ─────────────────────────────────────────
    private void agregarPunto(Point point, Style style) {
        puntosActuales.add(point);
        // Vértice en manager temporal
        Bitmap bmp = crearPuntoBitmap("#ff0000");
        managerTemporal.create(
                new PointAnnotationOptions()
                        .withPoint(point)
                        .withIconImage(bmp)
                        .withIconSize(0.8)
        );
        tvModo.setText("Puntos: " + puntosActuales.size() + " — toca Cerrar para terminar");
        if (puntosActuales.size() >= 2) dibujarLineaPrevia(style);
    }

    // ─────────────────────────────────────────
    // Cerrar EDIFICIO
    // ─────────────────────────────────────────
    private void cerrarEdificio() {
        edificioActualId++;
        puntosActuales.add(puntosActuales.get(0));
        puntosEdificioActual = new ArrayList<>(puntosActuales);

        mapView.getMapboxMap().getStyle(style ->
                dibujarPoligono(style, new ArrayList<>(puntosActuales),
                        "edificio-" + edificioActualId, "#2196F3", 0.25)
        );

        // Pin permanente en el centro
        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Edificio " + edificioActualId, "#1565C0");

        Toast.makeText(this, "Edificio " + edificioActualId + " guardado",
                Toast.LENGTH_SHORT).show();

        puntosActuales.clear();
        managerTemporal.deleteAll(); // ← solo borra vértices temporales
        modoActual = MODO_NINGUNO;
        btnEdificio.setText("Edificio");
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnAulas.setVisibility(View.VISIBLE);
        btnFinalizar.setVisibility(View.VISIBLE);
        tvModo.setText("Edificio guardado. Dibuja aulas o finaliza.");
    }

    // ─────────────────────────────────────────
    // Cerrar AULA
    // ─────────────────────────────────────────
    private void cerrarAula() {
        aulaContador++;
        puntosActuales.add(puntosActuales.get(0));

        String aulaId = "edificio-" + edificioActualId + "-aula-" + aulaContador;
        mapView.getMapboxMap().getStyle(style ->
                dibujarPoligono(style, new ArrayList<>(puntosActuales),
                        aulaId, "#FF9800", 0.4)
        );

        // Pin permanente en el centro
        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Aula " + aulaContador, "#E65100");

        Toast.makeText(this, "Aula " + aulaContador + " guardada",
                Toast.LENGTH_SHORT).show();

        puntosActuales.clear();
        managerTemporal.deleteAll(); // ← solo borra vértices temporales
        tvModo.setText("Aula " + aulaContador + " guardada. Dibuja la siguiente.");
    }

    //
    // ================ Pin permanente — usa managerPermanente ===================
    //
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

    // ─────────────────────────────────────────
    // Validar punto dentro del edificio
    // ─────────────────────────────────────────
    private boolean puntoDentroDeEdificio(Point punto) {
        if (puntosEdificioActual.size() < 3) return false;
        double x = punto.longitude();
        double y = punto.latitude();
        boolean inside = false;
        int n = puntosEdificioActual.size() - 1;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = puntosEdificioActual.get(i).longitude();
            double yi = puntosEdificioActual.get(i).latitude();
            double xj = puntosEdificioActual.get(j).longitude();
            double yj = puntosEdificioActual.get(j).latitude();
            if (((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }

    // ─────────────────────────────────────────
    // Calcular centro
    // ─────────────────────────────────────────
    private Point calcularCentro(List<Point> puntos) {
        double sumLng = 0, sumLat = 0;
        int total = puntos.size() - 1;
        for (int i = 0; i < total; i++) {
            sumLng += puntos.get(i).longitude();
            sumLat += puntos.get(i).latitude();
        }
        return Point.fromLngLat(sumLng / total, sumLat / total);
    }

    // ─────────────────────────────────────────
    // Dibujar polígono
    // ─────────────────────────────────────────
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

    // ─────────────────────────────────────────
    // Línea previa mientras dibuja
    // ─────────────────────────────────────────
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

    // ─────────────────────────────────────────
    // Refrescar solo vértices temporales
    // ─────────────────────────────────────────
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

    // ─────────────────────────────────────────
    // Bitmaps
    // ─────────────────────────────────────────
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

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
}