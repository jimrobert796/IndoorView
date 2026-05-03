package com.example.indoorview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraBoundsOptions;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.GesturesUtils;

public class MapaEventoActivity extends AppCompatActivity {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private MapManager mapManager;
    private Database db;
    private Spinner spinnerPisos;
    private Button btnFinalizar;
    private ActivityResultLauncher<Intent> camaraLauncher;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    // Datos del evento
    private int idEvento;
    private String nombreEvento;
    private String descripcionEvento;
    private String fechaInicio;
    private String horaInicio;
    private String fechaFin;
    private String horaFin;
    private String latitudEvento;
    private String longitudEvento;


    // ===== VARIABLES PARA MODO SELECCIÓN =====
    private boolean modoSeleccion = false;
    private Point puntoSeleccionado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_evento);

        // 1. Inicializar vistas
        mapView = findViewById(R.id.mapView);
        spinnerPisos = findViewById(R.id.spnPisos);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        // 2. Inicializar base de datos
        db = new Database(this);

        // 3. Verificar modo de operación
        verificarModo();

        // 4. Inicializar launchers
        inicializarLaunchers();

        // 5. Configurar el mapa
        configurarMapa();

        // 6. Configurar botón finalizar
        configurarBotonFinalizar();
    }

    /**
     * Verificar si viene en modo selección o visualización
     */
    private void verificarModo() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            modoSeleccion = extras.getBoolean("modo_seleccion", false);

            if (modoSeleccion) {
                // Modo selección: no cargar datos de evento
                Log.d("MAPA", "Modo SELECCIÓN activado");
                Toast.makeText(this, "👆 Toca en el mapa para seleccionar la ubicación", Toast.LENGTH_LONG).show();

                recibirDatosEvento();

                // Cambiar texto del botón
                btnFinalizar.setText("Confirmar Ubicación");
                btnFinalizar.setEnabled(false); // Deshabilitado hasta seleccionar
            } else {
                // Modo visualización: cargar datos del evento
                recibirDatosEvento();
                Log.d("MAPA", "Modo VISUALIZACIÓN");
            }
        }
    }

    /**
     * Recibir datos del evento para modo visualización
     */
    private void recibirDatosEvento() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            idEvento = extras.getInt("id_evento", -1);
            nombreEvento = extras.getString("nombre_evento", "");
            descripcionEvento = extras.getString("descripcion_evento", "");
            fechaInicio = extras.getString("fecha_inicio", "");
            horaInicio = extras.getString("hora_inicio", "");
            fechaFin = extras.getString("fecha_fin", "");
            horaFin = extras.getString("hora_fin", "");
            latitudEvento = extras.getString("latitud", "");
            longitudEvento = extras.getString("longitud", "");

            Log.d("EVENTO_RECIBIDO", "=================================");
            Log.d("EVENTO_RECIBIDO", "ID: " + idEvento);
            Log.d("EVENTO_RECIBIDO", "Nombre: " + nombreEvento);
            Log.d("EVENTO_RECIBIDO", "Coordenadas: " + latitudEvento + ", " + longitudEvento);
            Log.d("EVENTO_RECIBIDO", "=================================");
        }
    }

    /**
     * Cargar pin del evento (modo visualización)
     */
    private void cargarPinEvento() {
        if (mapManager == null || modoSeleccion) {
            return;
        }

        if (latitudEvento == null || longitudEvento == null ||
                latitudEvento.isEmpty() || longitudEvento.isEmpty() ||
                latitudEvento.equals("0.0") || longitudEvento.equals("0.0")) {
            Log.e("EVENTO", "Coordenadas inválidas");
            Toast.makeText(this, "Este evento no tiene ubicación asignada", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JsonObject datosEvento = new JsonObject();
            datosEvento.addProperty("id_evento", idEvento);
            datosEvento.addProperty("nombre", nombreEvento);
            datosEvento.addProperty("descripcion", descripcionEvento);
            datosEvento.addProperty("fecha_inicio", fechaInicio);
            datosEvento.addProperty("hora_inicio", horaInicio);
            datosEvento.addProperty("fecha_fin", fechaFin);
            datosEvento.addProperty("hora_fin", horaFin);
            datosEvento.addProperty("latitud", latitudEvento);
            datosEvento.addProperty("longitud", longitudEvento);

            Point punto = Point.fromLngLat(Double.parseDouble(longitudEvento), Double.parseDouble(latitudEvento));
            mapManager.agregarPinEvento(punto, nombreEvento, datosEvento);

            Toast.makeText(this, "📍 " + nombreEvento, Toast.LENGTH_SHORT).show();

            Log.e("EVENTO_PRUEBA", "coordenadas: " + longitudEvento+", "+ latitudEvento);

        } catch (NumberFormatException e) {
            Log.e("EVENTO", "Error al parsear coordenadas: " + e.getMessage());
            Toast.makeText(this, "Error al cargar la ubicación del evento", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Configurar botón finalizar según el modo
     */
    private void configurarBotonFinalizar() {
        btnFinalizar.setOnClickListener(v -> {
            if (modoSeleccion) {
                // Modo selección: confirmar la ubicación seleccionada
                mapManager.mostrarDialogoConfirmacion(
                        "Confirmación",
                        "¿Está seguro de guardar la ubicación seleccionada?",
                        "Sí",
                        () -> confirmarUbicacion()
                );
            } else {
                // Modo visualización: simplemente cerrar
                finish();
            }
        });
    }

    /**
     * Confirmar ubicación seleccionada y volver a AgregarEventoActivity
     */
    private void confirmarUbicacion() {
        if (puntoSeleccionado == null) {
            Toast.makeText(this, "⚠️ Por favor selecciona una ubicación", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preparar datos para devolver
        Intent resultIntent = new Intent();
        resultIntent.putExtra("latitud", String.valueOf(puntoSeleccionado.latitude()));
        resultIntent.putExtra("longitud", String.valueOf(puntoSeleccionado.longitude()));

        setResult(RESULT_OK, resultIntent);
        Toast.makeText(this, "✓ Ubicación confirmada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void inicializarLaunchers() {
        camaraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (mapManager != null) {
                            mapManager.procesarResultadoCamara(result.getData());
                        }
                    } else {
                        Toast.makeText(this, "Foto cancelada", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        if (mapManager != null) {
                            mapManager.procesarResultadoGaleria(result.getData());
                        }
                    } else {
                        Toast.makeText(this, "No se seleccionó imagen", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Configurar el mapa y detectar clicks DIRECTAMENTE
     */
    private void configurarMapa() {
        if (mapView == null) {
            Log.e("MAPA_ERROR", "mapView es null");
            return;
        }

        try {
            mapboxMap = mapView.getMapboxMap();

            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
                mapboxMap.setCamera(
                        new CameraOptions.Builder()
                                .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                                .zoom(17.0)
                                .build()
                );

                mapboxMap.setBounds(
                        new CameraBoundsOptions.Builder()
                                .minZoom(16.0)
                                .maxZoom(20.0)
                                .build()
                );

                style.setStyleLayerProperty("building", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("building-extrusion", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("landuse", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("landuse-overlay", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("pitch-outline", "visibility", Value.valueOf("none"));

                // Cargamos el estilo de mapa con todo y los poligonos SOLO VISTA
                mapManager = new MapManager(
                        mapView,
                        db,
                        this,
                        spinnerPisos,
                        camaraLauncher,
                        galeriaLauncher
                );
                // Solo cargar elementos en modo visualización
                mapManager.cargarLineStringDetalles();
                mapManager.cargarPoligonosLugar();
                mapManager.limpiarTodo();

                // Mostrar el pin que se supone tiene
                Point punto = Point.fromLngLat(Double.parseDouble(longitudEvento), Double.parseDouble(latitudEvento));
                mapManager.agregarPinEventoTemp(punto, nombreEvento);


                // ===== CONFIGURAR CLICK EN MAPA DIRECTAMENTE =====
                try {

                    GesturesPlugin gesturesPlugin = GesturesUtils.getGestures(mapView);
                    if (gesturesPlugin != null) {
                        // Crear listener de clicks
                        OnMapClickListener mapClickListener = point -> {
                            if (modoSeleccion) {



                                mapManager.limpiarPinEvento();


                                // Modo selección: capturar el click
                                puntoSeleccionado = point;
                                mapManager.agregarPinEventoTemp(puntoSeleccionado, nombreEvento);
                                btnFinalizar.setEnabled(true);
                                btnFinalizar.setBackgroundTintList(
                                        android.content.res.ColorStateList.valueOf(
                                                getResources().getColor(android.R.color.holo_green_light, null)
                                        )
                                );
                                Toast.makeText(MapaEventoActivity.this,
                                        "✓ Ubicación seleccionada: " + point.latitude() +
                                                ", " + point.longitude(),
                                        Toast.LENGTH_SHORT).show();
                                Log.d("MAPA_SELECCION", "Punto seleccionado: " + point.latitude() + ", " + point.longitude());
                                return true;
                            }
                            return false;
                        };

                        gesturesPlugin.addOnMapClickListener(mapClickListener);
                    }
                } catch (Exception e) {
                    Log.e("MAPA_ERROR", "Error al configurar gestos: " + e.getMessage());
                }

                // Crear MapManager para modo visualización
                if (mapManager == null) {
                    mapManager = new MapManager(
                            mapView,
                            db,
                            this,
                            spinnerPisos,
                            camaraLauncher,
                            galeriaLauncher
                    );
                }

                // Cargar elementos del mapa (solo en modo visualización)
                if (mapManager != null) {
                    if (!modoSeleccion) {

                        // Necesitamos que este boton no aparezca solo es vista
                        btnFinalizar.setVisibility(View.GONE);

                        // Solo cargar elementos en modo visualización
                        mapManager.cargarLineStringDetalles();
                        mapManager.cargarPoligonosLugar();
                        mapManager.limpiarTodo();
                        cargarPinEvento();
                    }
                } else {

                    Toast.makeText(this, "Error: MapManager no inicializado", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("MAPA_ERROR", "Error configurando mapa: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Ciclo de vida
    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }
}