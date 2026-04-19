package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class MapaFragment extends Fragment {

    private MapView mapView;
    private PointAnnotationManager managerPermanente;
    private PointAnnotationManager managerTemporal;

    private static final int MODO_NINGUNO = 0;
    private static final int MODO_LUGAR = 1;
    private static final int MODO_ESPACIO = 2;
    private int modoActual = MODO_NINGUNO;

    private List<Point> puntosActuales = new ArrayList<>();
    private List<Point> puntosLugarActual = new ArrayList<>();
    private int lugarActualId = 0;
    private int espacioContador = 0;

    private Button btnLugar, btnEspacios, btnCerrar, btnDeshacer, btnFinalizar, btnHabilitar;
    private TextView tvModo;

    private boolean modoEdicionActivo = false;
    private Database db;
    private MapManager mapManager;
    private int lugarSeleccionadoId = -1;
    private boolean espaciosVisibles = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        mapView = view.findViewById(R.id.mapView);
        btnLugar = view.findViewById(R.id.btnEditar);
        btnEspacios = view.findViewById(R.id.btnEspacios);
        btnCerrar = view.findViewById(R.id.btnCerrar);
        btnDeshacer = view.findViewById(R.id.btnDeshacer);
        btnFinalizar = view.findViewById(R.id.btnFinalizar);
        tvModo = view.findViewById(R.id.tvModo);
        btnHabilitar = view.findViewById(R.id.btnHabilitar);

        db = Database.getInstance(getActivity());
        mapManager = new MapManager(mapView, db, getActivity());
        mapManager.verificarConexionBD();

        configurarMapa();

        configurarBotones();

        return view;
    }

    private void configurarMapa() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                    .zoom(17.0)
                    .build());

            mapView.getMapboxMap().setBounds(new CameraBoundsOptions.Builder()
                    .minZoom(16.0)
                    .maxZoom(20.0)
                    .build());

            CoordinateBounds bounds = new CoordinateBounds(
                    Point.fromLngLat(-88.4195, 13.3435),
                    Point.fromLngLat(-88.4165, 13.3410));

            style.setStyleLayerProperty("building", "visibility", Value.valueOf("none"));
            style.setStyleLayerProperty("building-extrusion", "visibility", Value.valueOf("none"));

            mapManager.cargarPoligonosLugar();

            AnnotationPlugin annotationPlugin = mapView.getPlugin(Plugin.Mapbox.MAPBOX_ANNOTATION_PLUGIN_ID);
            managerPermanente = (PointAnnotationManager) annotationPlugin.createAnnotationManager(AnnotationType.PointAnnotation, null);
            managerTemporal = (PointAnnotationManager) annotationPlugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

            agregarPinPermanente(Point.fromLngLat(-88.41783453298294, 13.342296805328829), "UGB", "#0080ff");

            GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
                if (modoActual == MODO_LUGAR) {
                    agregarPunto(point, style);
                    return true;
                } else if (modoActual == MODO_ESPACIO) {
                    if (puntoDentroDeLugar(point)) {
                        agregarPunto(point, style);
                    } else {
                        Toast.makeText(getActivity(), "El punto está fuera del lugar", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });
        });
    }

    private void configurarBotones() {
        ocultarEdicion();

        btnHabilitar.setOnClickListener(v -> {
            if (modoEdicionActivo) {
                modoEdicionActivo = false;
                btnHabilitar.setText("HABILITAR");
                btnLugar.setVisibility(View.GONE);
                btnEspacios.setVisibility(View.GONE);
                btnCerrar.setVisibility(View.GONE);
                btnDeshacer.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.GONE);
                if (modoActual != MODO_NINGUNO) {
                    modoActual = MODO_NINGUNO;
                    btnLugar.setText("Lugar");
                    puntosActuales.clear();
                    refrescarVertices();
                }
                tvModo.setText("");
                Toast.makeText(getActivity(), "Modo edición desactivado", Toast.LENGTH_SHORT).show();
            } else {
                modoEdicionActivo = true;
                btnHabilitar.setText("CANCELAR");
                btnLugar.setVisibility(View.VISIBLE);
                btnEspacios.setVisibility(View.GONE);
                btnCerrar.setVisibility(View.GONE);
                btnDeshacer.setVisibility(View.GONE);
                btnFinalizar.setVisibility(View.GONE);
                tvModo.setText("Modo edición activado - Toca 'Lugar' para comenzar");
                Toast.makeText(getActivity(), "Modo edición activado", Toast.LENGTH_SHORT).show();
            }
        });

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

        btnCerrar.setOnClickListener(v -> {
            if (puntosActuales.size() < 3) {
                Toast.makeText(getActivity(), "Mínimo 3 puntos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (modoActual == MODO_LUGAR) cerrarLugar();
            else if (modoActual == MODO_ESPACIO) cerrarEspacio();
        });

        btnDeshacer.setOnClickListener(v -> {
            if (!puntosActuales.isEmpty()) {
                puntosActuales.remove(puntosActuales.size() - 1);
                refrescarVertices();
                tvModo.setText("Puntos: " + puntosActuales.size());
            }
        });

        btnFinalizar.setOnClickListener(v -> {
            modoActual = MODO_NINGUNO;
            ocultarBotonesEdicion();
            btnLugar.setText("Lugar");
            btnFinalizar.setVisibility(View.GONE);
            btnEspacios.setVisibility(View.GONE);
            puntosActuales.clear();
            managerTemporal.deleteAll();
            tvModo.setText("¡Listo! Lugar y espacios guardados.");
            Toast.makeText(getActivity(), "Mapa guardado correctamente", Toast.LENGTH_LONG).show();
        });
    }

    //  metodos que se utilizan
    // - ocultarEdicion()
    // - agregarPunto()
    // - cerrarLugar()
    // - cerrarEspacio()
    // - agregarPinPermanente()
    // - puntoDentroDeLugar()
    // - calcularCentro()
    // - dibujarPoligono()
    // - dibujarLineaPrevia()
    // - refrescarVertices()
    // - ocultarBotonesEdicion()
    // - crearPinBitmap()
    // - crearPuntoBitmap()
    // - mostrarGeoJsonEnDialogo()
    // - logGeoJson()
    // - generarGeoJsonDesdePuntos()


    private void ocultarEdicion() {
        btnLugar.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);
    }

    private void agregarPunto(Point point, Style style) {
        puntosActuales.add(point);
        Bitmap bmp = crearPuntoBitmap("#ff0000");
        managerTemporal.create(new PointAnnotationOptions().withPoint(point).withIconImage(bmp).withIconSize(0.8));
        tvModo.setText("Puntos: " + puntosActuales.size() + " — toca Cerrar para terminar");
        if (puntosActuales.size() >= 2) dibujarLineaPrevia(style);
    }

    private void cerrarLugar() {
        lugarActualId++;
        puntosActuales.add(puntosActuales.get(0));
        puntosLugarActual = new ArrayList<>(puntosActuales);
        String geojson = generarGeoJsonDesdePuntos(puntosActuales, "Lugar " + lugarActualId, "lugar", lugarActualId);
        mostrarGeoJsonEnDialogo("Lugar " + lugarActualId, geojson);
        logGeoJson("Lugar " + lugarActualId, geojson);
        mapView.getMapboxMap().getStyle(style -> dibujarPoligono(style, new ArrayList<>(puntosActuales), "lugar-" + lugarActualId, "#2196F3", 0.25));
        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Lugar " + lugarActualId, "#1565C0");
        Toast.makeText(getActivity(), "Lugar " + lugarActualId + " guardado", Toast.LENGTH_SHORT).show();
        puntosActuales.clear();
        managerTemporal.deleteAll();
        modoActual = MODO_NINGUNO;
        btnLugar.setText("Lugar");
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.VISIBLE);
        btnFinalizar.setVisibility(View.VISIBLE);
        tvModo.setText("Lugar guardado. Dibuja espacios o finaliza.");
    }

    private void cerrarEspacio() {
        espacioContador++;
        puntosActuales.add(puntosActuales.get(0));
        String geojson = generarGeoJsonDesdePuntos(puntosActuales, "Espacio " + espacioContador, "espacio", espacioContador);
        mostrarGeoJsonEnDialogo("Espacio " + espacioContador, geojson);
        logGeoJson("Espacio " + espacioContador, geojson);
        String espacioId = "lugar-" + lugarActualId + "-espacio-" + espacioContador;
        mapView.getMapboxMap().getStyle(style -> dibujarPoligono(style, new ArrayList<>(puntosActuales), espacioId, "#FF9800", 0.4));
        Point centro = calcularCentro(puntosActuales);
        agregarPinPermanente(centro, "Espacio " + espacioContador, "#E65100");
        Toast.makeText(getActivity(), "Espacio " + espacioContador + " guardada", Toast.LENGTH_SHORT).show();
        puntosActuales.clear();
        managerTemporal.deleteAll();
        tvModo.setText("Espacio " + espacioContador + " guardada. Dibuja la siguiente.");
    }

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
            Toast.makeText(getActivity(), "tocaste " + annotation.getTextField(), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

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
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
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

    private void dibujarPoligono(Style style, List<Point> puntos, String id, String color, double opacidad) {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < puntos.size(); i++) {
            Point p = puntos.get(i);
            coords.append("[").append(p.longitude()).append(",").append(p.latitude()).append("]");
            if (i < puntos.size() - 1) coords.append(",");
        }
        String geojson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[" + coords + "]]},\"properties\":{}}";
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

    private void dibujarLineaPrevia(Style style) {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < puntosActuales.size(); i++) {
            Point p = puntosActuales.get(i);
            coords.append("[").append(p.longitude()).append(",").append(p.latitude()).append("]");
            if (i < puntosActuales.size() - 1) coords.append(",");
        }
        String geojson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[" + coords + "]},\"properties\":{}}";
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

    private void refrescarVertices() {
        managerTemporal.deleteAll();
        for (Point p : puntosActuales) {
            Bitmap bmp = crearPuntoBitmap("#ff0000");
            managerTemporal.create(new PointAnnotationOptions().withPoint(p).withIconImage(bmp).withIconSize(0.8));
        }
    }

    private void ocultarBotonesEdicion() {
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
    }

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

    private void mostrarGeoJsonEnDialogo(String titulo, String geojson) {
        String geojsonMostrar = geojson.length() > 800 ? geojson.substring(0, 800) + "\n\n... (truncado)" : geojson;
        new AlertDialog.Builder(getActivity())
                .setTitle("📄 " + titulo)
                .setMessage(geojsonMostrar)
                .setPositiveButton("Copiar", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("GeoJSON", geojson);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getActivity(), "GeoJSON copiado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void logGeoJson(String titulo, String geojson) {
        Log.d("GEOJSON_DEBUG", "========== " + titulo + " ==========");
        Log.d("GEOJSON_DEBUG", geojson);
        Log.d("GEOJSON_DEBUG", "========== FIN ==========");
    }

    private String generarGeoJsonDesdePuntos(List<Point> puntos, String nombre, String tipo, int id) {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < puntos.size(); i++) {
            Point p = puntos.get(i);
            coords.append("[").append(p.longitude()).append(",").append(p.latitude()).append("]");
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

    @Override
    public void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    public void onStop() { super.onStop(); mapView.onStop(); }
    @Override
    public void onDestroyView() { super.onDestroyView(); mapView.onDestroy(); }
}