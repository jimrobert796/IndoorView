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
    private Context context;  // Añadir Context

    private PointAnnotationManager managerLugares;
    private PointAnnotationManager managerEspacios;

    public int lugarSeleccionado = -1;

    // Para saber si esta en modoEdicion
    public boolean modoEdicion = false;


    // Lista para registrar todas las fuentes/capas de espacios
    private List<String> capasEspaciosRegistradas = new ArrayList<>();

    // Lista para registrar todas las fuentes/capas de lugares
    private List<String> capasLugaresRegistradas = new ArrayList<>();

    // Para poder ocultar el Pin sin perder informacion
    List<PointAnnotation> pinesOcultos = new ArrayList<>();

    // Solucionador de bug
    private PointAnnotation pinOcultoActual = null;


    // Para manejo de pisos
    Spinner spinnerPisos;



    // Constructor que necesita el MapView, la instancia bd y el contexto en donde
    public MapManager(MapView mapView, Database db, Context context, Spinner spinner) {
        this.mapView = mapView;
        this.db = db;
        this.context = context;
        this.spinnerPisos = spinner;

        // DEBUG
        if (spinnerPisos == null) {
            Log.e("SPINNER_ERROR", "¡El spinner es NULL en MapManager!");
        } else {
            Log.d("SPINNER_DEBUG", "✓ Spinner inicializado correctamente");
            Log.d("SPINNER_DEBUG", "Visibility: " + spinnerPisos.getVisibility());
            Log.d("SPINNER_DEBUG", "Width: " + spinnerPisos.getWidth());
            Log.d("SPINNER_DEBUG", "Height: " + spinnerPisos.getHeight());
        }

        // Crear dos managers separados
        AnnotationPlugin plugin = mapView.getPlugin(Plugin.Mapbox.MAPBOX_ANNOTATION_PLUGIN_ID);

        managerLugares = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        managerEspacios = (PointAnnotationManager)
                plugin.createAnnotationManager(AnnotationType.PointAnnotation, null);

        // ================== EVENTOS DE MAPA ==========================

        managerLugares.addClickListener(annotation -> {
            // Obtenemos el punto de referencia
            Point punto = annotation.getPoint();

            // Llamamos la redirecion al punto
            redireccionPin(punto);

            // Usaremos jsonObject para solo pasar los datos necesarios
            JsonObject data = (JsonObject) annotation.getData();

            // Restauramos pines ocultos a la hora de buscar
            restaurarPinesOcultos();


            // Condicional para Crud
            if (modoEdicion) {

                // Aqui se hara inicio al crud
                Toast.makeText(context, "Listo para modificar CRUD", Toast.LENGTH_SHORT).show();

            } else {

                // Aqui se abre el .XML para mostrar la informacion
                mostrarInfoLugar(data);

                limpiarEspacios(); // Limpia el estilo
                lugarSeleccionado = -1;  // Variable Global para saber el lugar selecionado

                Toast.makeText(context, "Espacios ocultos", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // mantener presionado muestra los espacios
        managerLugares.addLongClickListener(annotation -> {
            // Obtenemos el punto de referencia
            Point punto = annotation.getPoint();

            // Llamamos la redirecion al punto
            redireccionPin(punto);

            // Usaremos jsonObject para solo pasar los datos necesarios
            JsonObject data = (JsonObject) annotation.getData();

            if (data != null) {
                // Obteher el id_lugar del data.json
                int idLugar = data.get("id_lugar").getAsInt();

                // Si el pin ocultado anteriormente tiene informacion se muestra
                if (pinOcultoActual != null) {
                    pinOcultoActual.setIconSize(0.9);
                    pinOcultoActual.setTextOpacity(1.0);
                    managerLugares.update(pinOcultoActual);
                    pinOcultoActual = null;
                }

                // Verificar si ya está seleccionado usando una variable local
                if (lugarSeleccionado != idLugar) {

                    // Agregar a pines ocultos
                    pinesOcultos.add(annotation);

                    //ocultar el pin
                    annotation.setIconSize(0.0);
                    annotation.setTextOpacity(0.0);
                    managerLugares.update(annotation);

                    // Pin oculto actual sera ahora este
                    pinOcultoActual = annotation;

                    // Limpiar espacios anteriores
                    limpiarEspacios();
                    limpiarEspaciosDeLugar(lugarSeleccionado);

                    // Cargar los pisos por el lugar selecionado
                    cargarPisos(idLugar);

                    // Mostrar espacios del lugar
                    // mostrarEspacios(idLugar);

                    lugarSeleccionado = idLugar;

                    Toast.makeText(context, "Mostrando espacios del lugar", Toast.LENGTH_SHORT).show();
                } else {

                    // Ocultar espacios
                    limpiarEspacios();
                    // limpiarEspaciosDeLugar(idLugar);

                    lugarSeleccionado = -1;

                    Toast.makeText(context, "Ocultando espacios", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Error: Datos no disponibles", Toast.LENGTH_SHORT).show();
            }

            return true;
        });

        // ============= Evento Click en Espacio ===============
        managerEspacios.addClickListener(annotation -> {
            // Obtenemos el punto de referencia
            Point punto = annotation.getPoint();

            // Llamamos la redirecion al punto
            redireccionPin(punto);

            // Usaremos jsonObject para solo pasar los datos necesarios
            JsonObject data = (JsonObject) annotation.getData();

            // Obtener el id_espacin
            int idEspacio = data.get("id_espacio").getAsInt();

            // Condicional para Crud
            if (modoEdicion) {

                // Aqui se iniciara el crud de edicion de espacio
                Toast.makeText(context, "Listo para modificar CRUD", Toast.LENGTH_SHORT).show();


            } else {
                Toast.makeText(context, "MOSTRANDO", Toast.LENGTH_SHORT).show();
                // Aqui se muestra el .XML con la info nececesaria
                mostrarInfoEspacio(data);
            }
            return true;
        });


    }

    // Cambiar el estilo normal a los pines
    private void restaurarPinesOcultos() {
        for (PointAnnotation pin : pinesOcultos) {
            pin.setIconSize(0.9);
            pin.setTextOpacity(1.0);
            managerLugares.update(pin);
        }
        pinesOcultos.clear();
    }

    // Para poder modificar el modo de edicion
    public void setModoEdicion(boolean activar) {
        this.modoEdicion = activar;
    }

    public boolean isModoEdicion() {
        return modoEdicion;
    }




    // =====  redirecionar el punto o pin selecionado =======
    private void redireccionPin(Point punto){

        CameraAnimationsPlugin animationPlugin =
                mapView.getPlugin(com.mapbox.maps.plugin.Plugin.MAPBOX_CAMERA_PLUGIN_ID);

        animationPlugin.easeTo(
                new CameraOptions.Builder()
                        .center(punto)
                        .zoom(19.8)
                        .build(),
                new MapAnimationOptions.Builder()
                        .duration(1200L) // TIEMPO DE ANIMACION
                        .build(),
                null
        );
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

            List<Espacio> espacios = db.getEspaciosByLugar(lugar.getId_lugar());
            for (Espacio espacio : espacios) {
                mensaje.append("    ").append(espacio.getNombre()).append("\n");
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

        // Registrar la capa
        if (!capasLugaresRegistradas.contains(sourceId)) {
            capasLugaresRegistradas.add(sourceId);
        }



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
                        lugar
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

        if (!capasEspaciosRegistradas.contains(sourceId)) {
            capasEspaciosRegistradas.add(sourceId);
        }



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
                            espacio,
                            vertices
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

    private void agregarPinLugar(Point punto, String texto, String color, Lugar lugar) {
        Bitmap icono = crearPinBitmap(color);

        JsonObject data = new JsonObject();
        data.addProperty("id_lugar", lugar.getId_lugar());
        data.addProperty("nombre", lugar.getNombre());
        data.addProperty("descripcion", lugar.getDescripcion());
        data.addProperty("url_imagenes", lugar.getUrl_imagenes());
        data.addProperty("estado", lugar.getEstado());
        data.addProperty("geojson", lugar.getGeojson());


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

    // Funcion que llama el array de espacios en Array para limpieza
    public void limpiarEspacios() {
        managerEspacios.deleteAll();

        // 2. Limpiar capas registradas
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




    // Quita los vertices y pines del mapa temporal en Array
    public void limpiarLugares() {
        // 1. Limpiar pines de lugares
        managerLugares.deleteAll();

        // 2. Limpiar todas las capas de polígonos de lugares
        mapView.getMapboxMap().getStyle(style -> {
            for (String sourceId : capasLugaresRegistradas) {
                // Eliminar capa de relleno
                if (style.styleLayerExists(sourceId + "-fill")) {
                    style.removeStyleLayer(sourceId + "-fill");
                }
                // Eliminar capa de línea
                if (style.styleLayerExists(sourceId + "-line")) {
                    style.removeStyleLayer(sourceId + "-line");
                }
                // Eliminar fuente
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

    private void mostrarLugares() {
        List<Lugar> lugares = db.getLugares(); // Ya filtra por estado = 1

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






    // Busca los esapcios para luego encontrar la geometria
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


    private void mostrarInfoLugar(JsonObject data) {
        // Extraer datos del JSON
        int idLugar = data.get("id_lugar").getAsInt();

        String nombre = data.get("nombre").getAsString();
        String descripcion = data.get("descripcion").getAsString();
        String url_imagenes = data.get("url_imagenes").getAsString();
        int estado = data.get("estado").getAsInt();
        String geojson = data.get("geojson").getAsString();

        // Construir mensaje
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("INFORMACION DEL LUGAR\n\n");
        mensaje.append("Nombre: ").append(nombre).append("\n");
        mensaje.append("Descripcion: ").append(descripcion).append("\n");
        mensaje.append("Imagenes: ").append(url_imagenes).append("\n");
        mensaje.append("ID Lugar: ").append(idLugar).append("\n");
        mensaje.append("Estado: ").append(estado).append("\n");
        mensaje.append("geojson ").append(geojson).append("\n");

        new AlertDialog.Builder(context)
                .setTitle("Informacion: " + nombre)
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }


// DIALOGO PARA MOSTRAR INFORMACION DE UN ESPACIO (AULA)

    private void mostrarInfoEspacio(JsonObject data) {
        // Extraer datos del JSON
        int idEspacio = data.get("id_espacio").getAsInt();
        int idLugar = data.get("id_lugar").getAsInt();
        int idPiso = data.get("id_piso").getAsInt();

        String nombre = data.get("nombre").getAsString();
        String descripcion = data.get("descripcion").getAsString();
        String url_imagenes = data.get("url_imagenes").getAsString();
        int estado = data.get("estado").getAsInt();
        String vertices = data.get("vertices").getAsString();

        // Construir mensaje
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("INFORMACION DEL ESPACIO\n\n");
        mensaje.append("Nombre: ").append(nombre).append("\n");
        mensaje.append("Descripcion: ").append(descripcion).append("\n");
        mensaje.append("Imagenes: ").append(url_imagenes).append("\n");
        mensaje.append("ID: ").append(idEspacio).append("\n");
        mensaje.append("ID Lugar: ").append(idLugar).append("\n");
        mensaje.append("ID Piso: ").append(idPiso).append("\n");
        mensaje.append("Estado: ").append(estado).append("\n");
        mensaje.append("Vertices ").append(vertices).append("\n");

        new AlertDialog.Builder(context)
                .setTitle("Informacion: " + nombre)
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    public void cargarPisos(int idLugar) {
        List<Pisos> pisos = db.getPisosByLugar(idLugar);
        List<Integer> listaPisos = new ArrayList<>();
        List<Integer> listaPisosId = new ArrayList<>();

        Log.d("DEBUG_PISO", "=== CARGANDO PISOS ===");
        Log.d("DEBUG_PISO", "ID Lugar: " + idLugar);

        for (Pisos p : pisos) {
            Log.d("DEBUG_PISO", "Piso: ID=" + p.getId_piso() + " | Número=" + p.getNumero());
            listaPisos.add(p.getNumero());
            listaPisosId.add(p.getId_piso());
        }

        Log.d("DEBUG_PISO", "Números en spinner: " + listaPisos.toString());
        Log.d("DEBUG_PISO", "IDs reales: " + listaPisosId.toString());

        // MOSTRAR SPINNER SOLO SI HAY MAS DE 1 PISO
        if (listaPisos.size() > 1) {
            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                    context,
                    android.R.layout.simple_spinner_item,
                    listaPisos
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPisos.setAdapter(adapter);

            spinnerPisos.setTag(listaPisosId);

            spinnerPisos.setVisibility(View.VISIBLE);  // ← MOSTRAR

            Toast.makeText(context, "Selecciona un piso", Toast.LENGTH_SHORT).show();

        } else if (listaPisos.size() == 1) {
            // Si hay solo 1 piso, ocultar spinner y mostrar espacios del único piso
            spinnerPisos.setVisibility(View.GONE);  // ← OCULTAR AQUÍ
            mostrarEspaciosPorPiso(idLugar, listaPisosId.get(0));

            Toast.makeText(context, "Mostrando espacios (1 piso)", Toast.LENGTH_SHORT).show();

        } else {
            // Si no hay pisos
            spinnerPisos.setVisibility(View.GONE);  // ← OCULTAR AQUÍ TAMBIÉN
            Toast.makeText(context, "No hay pisos registrados", Toast.LENGTH_SHORT).show();
        }
    }

    // Mostrar espacios filtrados por piso - CON DEBUGGING
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
            // ← LOG DE CADA ESPACIO
            Log.d("DEBUG_PISO",
                    "Espacio: " + espacio.getNombre() +
                            " | ID: " + espacio.getId_espacio() +
                            " | Piso: " + espacio.getId_piso());

            // Filtrar por piso
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
            Log.d("DEBUG_PISO", "⚠️ NINGÚN ESPACIO COINCIDE CON EL PISO");
        }
    }




}