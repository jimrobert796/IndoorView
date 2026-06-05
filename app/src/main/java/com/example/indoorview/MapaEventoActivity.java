package com.example.indoorview;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraBoundsOptions;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;

public class MapaEventoActivity extends AppCompatActivity {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private MapManager mapManager;
    private Database db;
    private Spinner spinnerPisos;
    private Button btnFinalizar;
    private FloatingActionButton btnGiroscopio;
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


    // ===== VARIABLES GIROSCOPIO =====
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private SensorEventListener rotationListener;

    private boolean seguimientoDireccion = false;
    private float ultimoBearing = 0f;

    // ===== VARIABLES UBICACIÓN =====
    private LocationComponentPlugin locationComponent;

    // ========= Variables para coordenadas de usuario ========
    private double latitudUsuario = 0.0;
    private double longitudUsuario = 0.0;

    private Location ultimaUbicacionValida = null;
    private long ultimoTiempoUbicacion = 0;
    private boolean primeraVezCentrado = false;


    ///  RECORDA HACER QUE EL GIRO FUNCIONE ES L0 ULTIMO QUE NECESITO PARA PULIR BIEN ESTA APP




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_evento);

        // 1. Inicializar vistas
        mapView = findViewById(R.id.mapView);
        spinnerPisos = findViewById(R.id.spnPisos);
        btnFinalizar = findViewById(R.id.btnFinalizar);
        btnGiroscopio = findViewById(R.id.btnGiroscopio); // Agrega este botón en tu XML

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

        // 7. Habilitar boton de girsocopio
        configurarBotonGiroscopio();
    }

    private void inicializarBrujula() {

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        if (rotationSensor == null) {
            Toast.makeText(this,
                    "El dispositivo no tiene sensor de orientación",
                    Toast.LENGTH_LONG).show();
            return;
        }

        rotationListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (!seguimientoDireccion || mapboxMap == null) {
                    return;
                }

                float[] rotationMatrix = new float[9];
                float[] orientationAngles = new float[3];

                SensorManager.getRotationMatrixFromVector(
                        rotationMatrix,
                        event.values
                );

                SensorManager.getOrientation(
                        rotationMatrix,
                        orientationAngles
                );

                float azimuthInRadians = orientationAngles[0];
                float azimuthInDegrees =
                        (float) Math.toDegrees(azimuthInRadians);

                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360;
                }

                // Suavizar movimiento
                float diferencia = Math.abs(azimuthInDegrees - ultimoBearing);

                if (diferencia > 2) {

                    ultimoBearing = azimuthInDegrees;

                    mapboxMap.setCamera(
                            new CameraOptions.Builder()
                                    .bearing((double) azimuthInDegrees)
                                    .build()
                    );
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }




    private void activarUbicacionUsuario() {
        PermissionManager.getInstance().requestNotificationAndLocationPermissions(this,
                new PermissionManager.PermissionCallback() {
                    @Override
                    public void onPermissionGranted(int requestCode) {}

                    @Override
                    public void onPermissionDenied(int requestCode) {
                        Toast.makeText(MapaEventoActivity.this,
                                "Se necesita permiso de ubicación", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAllPermissionsGranted() {
                        mostrarUbicacionEnMapa();
                        // Activar brújula
                        inicializarBrujula();
                    }

                    @Override
                    public void onSomePermissionsDenied(String[] permissions, int[] grantResults) {
                        if (PermissionManager.getInstance().hasLocationPermission(MapaEventoActivity.this)) {
                            mostrarUbicacionEnMapa();
                            // Activar brújula
                            inicializarBrujula();
                        }
                    }
                });
    }


    private void mostrarUbicacionEnMapa() {

        try {

            locationComponent = LocationComponentUtils.getLocationComponent(mapView);

            if (locationComponent != null) {

                locationComponent.setEnabled(true);
                locationComponent.setPulsingEnabled(true);
                locationComponent.setPuckBearingEnabled(false);

                locationComponent.addOnIndicatorPositionChangedListener(point -> {

                    Location nuevaUbicacion = new Location("Mapbox");
                    nuevaUbicacion.setLatitude(point.latitude());
                    nuevaUbicacion.setLongitude(point.longitude());

                    long tiempoActual = System.currentTimeMillis();

                    if (ultimaUbicacionValida != null) {

                        ultimaUbicacionValida.getAccuracy();

                        float distancia =
                                ultimaUbicacionValida.distanceTo(nuevaUbicacion);

                        long diferenciaTiempo =
                                tiempoActual - ultimoTiempoUbicacion;

                        // Ignorar saltos absurdos
                        if (distancia > 2.5 ) {

                            Log.d(
                                    "GPS_FILTRO",
                                    "Salto ignorado: " + distancia + " metros"
                            );

                            return;
                        }
                    }

                    latitudUsuario = point.latitude();
                    longitudUsuario = point.longitude();

                    ultimaUbicacionValida = nuevaUbicacion;
                    ultimoTiempoUbicacion = tiempoActual;

                    // AQUÍ: ya tenemos coordenadas válidas guardadas para primera vez
                    if (!primeraVezCentrado && seguimientoDireccion) {
                        primeraVezCentrado = true;
                        runOnUiThread(() -> centrarEnUsuario());
                    }

                    Log.d(
                            "GPS",
                            latitudUsuario + ", " + longitudUsuario
                    );
                });

                Log.d("UBICACION", "✅ Ubicación del usuario activada");
            }

        } catch (Exception e) {

            Log.e(
                    "UBICACION",
                    "Error activando ubicación: " + e.getMessage()
            );
        }
    }


    private void centrarEnUsuario() {

        if (mapboxMap == null) {
            return;
        }

        if (latitudUsuario == 0.0 || longitudUsuario == 0.0) {

            Toast.makeText(this,
                    "Esperando ubicación...",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        Point ubicacionUsuario =
                Point.fromLngLat(longitudUsuario, latitudUsuario);

        CameraAnimationsPlugin animationPlugin =
                mapView.getPlugin(Plugin.MAPBOX_CAMERA_PLUGIN_ID);

        animationPlugin.easeTo(
                new CameraOptions.Builder()
                        .center(ubicacionUsuario)
                        .zoom(19.0)
                        .bearing((double) ultimoBearing)
                        .pitch(45.0)
                        .build(),

                new MapAnimationOptions.Builder()
                        .duration(1500L)
                        .build(),

                null
        );
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
                Toast.makeText(this, "Toca en el mapa para seleccionar la ubicación", Toast.LENGTH_LONG).show();

                recibirDatosEvento();

                // Cambiar texto del botón
                btnFinalizar.setText("Confirmar Ubicación");
                btnFinalizar.setEnabled(false); // Deshabilitado hasta seleccionar
                btnGiroscopio.setVisibility(View.GONE);
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

            redireccionPin(punto);

            Log.e("EVENTO_PRUEBA", "coordenadas: " + longitudEvento+", "+ latitudEvento);

        } catch (NumberFormatException e) {
            Log.e("EVENTO", "Error al parsear coordenadas: " + e.getMessage());
            Toast.makeText(this, "Error al cargar la ubicación del evento", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Centrar la cámara en el pin del evento
     */
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

    /*
    private void configurarBotonGiroscopio() {
        if (!modoSeleccion){
            btnGiroscopio.setOnClickListener(v -> {

                activarUbicacionUsuario();

                seguimientoDireccion = !seguimientoDireccion;

                if (seguimientoDireccion) {

                    inicializarBrujula();

                    if (sensorManager != null &&
                            rotationSensor != null &&
                            rotationListener != null) {

                        sensorManager.registerListener(
                                rotationListener,
                                rotationSensor,
                                SensorManager.SENSOR_DELAY_UI
                        );
                    }

                    // QUITAR: centrarEnUsuario();
                    // El centrado ahora lo maneja el listener cuando llega la primera ubicación

                    Toast.makeText(this, "Brujula activada", Toast.LENGTH_SHORT).show();

                } else {

                    primeraVezCentrado = false; // ← resetear para próxima activación

                    if (sensorManager != null && rotationListener != null) {
                        sensorManager.unregisterListener(rotationListener);
                    }

                    if (locationComponent != null) {
                        locationComponent.updateSettings(settings -> {
                            settings.setEnabled(false);
                            return null;
                        });
                    }

                    Toast.makeText(this, "Brujula desactivada", Toast.LENGTH_SHORT).show();
                }

            });
        }

    }

     */



    ///  SE UTILIZARA DESPUES CUANDO ESTEMOS EN LA INST PARA HACER PRUEBAS DE COORDENADAS

    private void configurarBotonGiroscopio() {
    if (!modoSeleccion){
        btnGiroscopio.setOnClickListener(v -> {

            // activamos la ubicacion en real time
            activarUbicacionUsuario();


            // Verificar si el usuario está dentro de la institución
            if (!mapManager.usuarioDentroDeInstitucion(latitudUsuario, longitudUsuario)) {
            //if (false) {
                Toast.makeText(this,
                    "La Ubicacion solo funciona dentro de la institución",
                    Toast.LENGTH_LONG).show();

                // Si estaba activada, desactivarla
                if (seguimientoDireccion) {
                    seguimientoDireccion = false;
                    if (sensorManager != null && rotationListener != null) {
                        sensorManager.unregisterListener(rotationListener);
                    }
                    btnGiroscopio.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#2196F3")
                            )
                    );
                }
                return;
            }

            // Alternar estado de seguimiento
            seguimientoDireccion = !seguimientoDireccion;

            if (seguimientoDireccion) {



                if (sensorManager != null && rotationSensor != null && rotationListener != null) {
                    sensorManager.registerListener(
                        rotationListener,
                        rotationSensor,
                        SensorManager.SENSOR_DELAY_UI
                    );


                    // Cambiar color del botón para indicar que está activo
                    btnGiroscopio.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    getResources().getColor(android.R.color.holo_green_light, null)
                            )
                    );

                    Toast.makeText(this,
                            "Ubicacion activada - Siguiendo tu orientación",
                            Toast.LENGTH_SHORT).show();
                }else{
                    seguimientoDireccion = !seguimientoDireccion;
                    // Desactivar brújula
                    if (sensorManager != null && rotationListener != null) {
                        sensorManager.unregisterListener(rotationListener);
                    }

                }
            } else {

                primeraVezCentrado = false; // ← resetear para próxima activación

                // Desactivar brújula
                if (sensorManager != null && rotationListener != null) {
                    sensorManager.unregisterListener(rotationListener);
                }

                // desactivar el puck de ubicación
                if (locationComponent != null) {
                    locationComponent.updateSettings(settings -> {
                        settings.setEnabled(false);
                        return null;
                    });
                }

                // Restaurar color original
                btnGiroscopio.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#2196F3")
                        )
                );

                Toast.makeText(this,
                    "Ubicacion desactivada",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}






    /**
     * Confirmar ubicación seleccionada y volver a AgregarEventoActivity
     */
    private void confirmarUbicacion() {
        if (puntoSeleccionado == null) {
            Toast.makeText(this, "⚠️ Por favor selecciona una ubicación", Toast.LENGTH_SHORT).show();
            return;
        }

        // VALIDACIÓN ADICIONAL: Double-check que esté dentro del UGB
        if (!mapManager.puntoDentroDeInstitucion(puntoSeleccionado)) {
            Toast.makeText(this,
                    "La ubicación está fuera de los límites permitidos",
                    Toast.LENGTH_LONG).show();
            Log.e("EVENTO_CONFIRM", "Ubicación confirmada fuera del UGB");
            return;
        }

        Log.d("EVENTO_CONFIRM", "✅ Ubicación confirmada dentro del UGB");

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
                // Configurar cámara y límites
                mapboxMap.setCamera(new CameraOptions.Builder()
                        .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                        .zoom(17.0)
                        .pitch(45.0)
                        .build());

                mapboxMap.setBounds(new CameraBoundsOptions.Builder()
                        .minZoom(16.0)
                        .maxZoom(20.0)
                        .build());

                // Ocultar capas no deseadas
                style.setStyleLayerProperty("building", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("building-extrusion", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("landuse", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("landuse-overlay", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("pitch-outline", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty(
                        "poi-label",
                        "visibility",
                        Value.valueOf("none")
                );

                //activarUbicacionUsuario();


                // CREAR UNA SOLA INSTANCIA de MapManager
                if (mapManager == null) {
                    mapManager = new MapManager(mapView, db, this, spinnerPisos, camaraLauncher, galeriaLauncher);
                }

                // Configurar según el modo
                if (modoSeleccion) {
                    // SOLO mostrar pin si hay coordenadas VÁLIDAS (no 0.0 ni vacío)
                    if (latitudEvento != null && longitudEvento != null &&
                            !latitudEvento.isEmpty() && !longitudEvento.isEmpty() &&
                            !latitudEvento.equals("0.0") && !longitudEvento.equals("0.0")) {

                        try {
                            Point punto = Point.fromLngLat(Double.parseDouble(longitudEvento), Double.parseDouble(latitudEvento));
                            mapManager.agregarPinEventoTemp(punto, nombreEvento);

                        } catch (NumberFormatException e) {
                            Log.e("MAPA", "Error al parsear coordenadas: " + e.getMessage());
                        }
                    }

                        // Modo selección: configurar click en mapa
                    configurarClickEnMapa(style);
                    mapManager.cargarLineStringDetalles();
                    mapManager.cargarPoligonosLugar();
                    mapManager.limpiarTodo();
                    btnFinalizar.setVisibility(View.VISIBLE);
                    btnFinalizar.setEnabled(false);
                    btnFinalizar.setText("Confirmar Ubicación");
                } else {
                    // Modo visualización: cargar elementos existentes
                    btnFinalizar.setVisibility(View.GONE);
                    mapManager.cargarLineStringDetalles();
                    mapManager.cargarPoligonosLugar();
                    mapManager.limpiarTodo();
                    cargarPinEvento(); // Cargar pin después de que mapManager existe
                }
            });
        } catch (Exception e) {
            Log.e("MAPA_ERROR", "Error configurando mapa: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Configurar click en mapa para modo selección
     */
    private void configurarClickEnMapa(Style style) {
        try {
            GesturesPlugin gesturesPlugin = GesturesUtils.getGestures(mapView);
            if (gesturesPlugin != null) {
                OnMapClickListener mapClickListener = point -> {
                    if (modoSeleccion && mapManager != null) {
                        // VALIDACIÓN: Verificar que el punto esté dentro del UGB
                        if (!mapManager.puntoDentroDeInstitucion(point)) {
                            Toast.makeText(MapaEventoActivity.this,
                                    "El evento está FUERA de los límites permitidos",
                                    Toast.LENGTH_LONG).show();
                            Log.e("EVENTO_UGB", "Punto fuera de UGB: [" + point.longitude() + ", " + point.latitude() + "]");
                            return true;
                        }

                        Log.d("EVENTO_UGB", "Punto VÁLIDO dentro del UGB");

                        // Limpiar pin anterior
                        mapManager.limpiarPinEvento();

                        // Guardar punto seleccionado
                        puntoSeleccionado = point;

                        // Mostrar pin temporal
                        mapManager.agregarPinEventoTemp(puntoSeleccionado,
                                nombreEvento != null ? nombreEvento : "Nuevo evento");

                        // Habilitar botón finalizar
                        btnFinalizar.setEnabled(true);
                        btnFinalizar.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(
                                        getResources().getColor(android.R.color.holo_green_light)
                                )
                        );

                        Toast.makeText(MapaEventoActivity.this,
                                "✓ Ubicación seleccionada: " + String.format("%.6f", point.latitude()) +
                                        ", " + String.format("%.6f", point.longitude()),
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                };
                gesturesPlugin.addOnMapClickListener(mapClickListener);
            }
        } catch (Exception e) {
            Log.e("MAPA_ERROR", "Error configurar click: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // CICLO DE VIDA
    // ════════════════════════════════════════════════════════════════

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
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

    // para el Giroscopio
    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null && rotationSensor != null && rotationListener != null) {

            sensorManager.registerListener(
                    rotationListener,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null && rotationListener != null) {
            sensorManager.unregisterListener(rotationListener);
        }
    }


}