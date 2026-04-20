package com.example.indoorview;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraBoundsOptions;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.CoordinateBounds;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ════════════════════════════════════════════════════════════════
    // VARIABLES DE MAPA Y UI
    // ════════════════════════════════════════════════════════════════
    private MapView mapView;
    private MapManager mapManager;
    private Database db;

    // UI Elements
    private Button btnLugar, btnEspacios, btnCerrar, btnDeshacer, btnFinalizar, btnHabilitar;
    private TextView tvModo;
    private Spinner spinnerPisos;

    // ════════════════════════════════════════════════════════════════
    // MODOS DE OPERACIÓN
    // ════════════════════════════════════════════════════════════════
    private static final int MODO_NINGUNO = 0;
    private static final int MODO_LUGAR = 1;
    private static final int MODO_ESPACIO = 2;
    private int modoActual = MODO_NINGUNO;

    private boolean modoEdicionActivo = false;

    // Manager permanente para UGB pin
    private PointAnnotationManager managerPermanente;

    // ════════════════════════════════════════════════════════════════
    // onCreate
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar UI elementos
        inicializarUI();

        // Inicializar base de datos
        db = Database.getInstance(this);

        // Inicializar MapManager
        mapManager = new MapManager(mapView, db, this, spinnerPisos);

        // Verificar conexión BD
        mapManager.verificarConexionBD();

        // Configurar spinner de pisos
        configurarSpinner();

        // Configurar mapa
        configurarMapa();

        // Configurar listeners de botones
        configurarBotones();

        // Ocultar UI de edición por defecto
        ocultarEdicion();
        spinnerPisos.setVisibility(View.GONE);
    }

    // ════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ════════════════════════════════════════════════════════════════

    /**
     * Inicializar elementos de UI desde activity_main.xml
     */
    private void inicializarUI() {
        mapView = findViewById(R.id.mapView);
        btnLugar = findViewById(R.id.btnEditar);
        btnEspacios = findViewById(R.id.btnEspacios);
        btnCerrar = findViewById(R.id.btnCerrar);
        btnDeshacer = findViewById(R.id.btnDeshacer);
        btnFinalizar = findViewById(R.id.btnFinalizar);
        btnHabilitar = findViewById(R.id.btnHabilitar);
        tvModo = findViewById(R.id.tvModo);
        spinnerPisos = findViewById(R.id.spnPisos);
    }

    /**
     * Configurar el spinner de pisos
     */
    private void configurarSpinner() {
        spinnerPisos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                List<Integer> pisosId = (List<Integer>) spinnerPisos.getTag();

                if (pisosId != null && position < pisosId.size()) {
                    int pisoIdReal = pisosId.get(position);

                    Log.d("DEBUG_PISO", "Position: " + position +
                            " | Número mostrado: " + parent.getItemAtPosition(position) +
                            " | ID real: " + pisoIdReal);

                    if (mapManager.lugarSeleccionado != -1) {
                        mapManager.limpiarEspacios();
                        mapManager.mostrarEspaciosPorPiso(mapManager.lugarSeleccionado, pisoIdReal);
                        Toast.makeText(MainActivity.this, "Mostrando piso " +
                                parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Configurar el mapa
     */
    private void configurarMapa() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {

            // Posicionar cámara
            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                            .zoom(17.0)
                            .build()
            );

            // Límites de zoom
            mapView.getMapboxMap().setBounds(
                    new CameraBoundsOptions.Builder()
                            .minZoom(16.0)
                            .maxZoom(20.0)
                            .build()
            );

            // Desactivar edificios por defecto de OpenStreetMap
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

            // Cargar polígonos de lugares
            mapManager.cargarPoligonosLugar();

            // Crear manager permanente para UGB pin
            AnnotationPlugin annotationPlugin = mapView.getPlugin(Plugin.Mapbox.MAPBOX_ANNOTATION_PLUGIN_ID);
            managerPermanente = (PointAnnotationManager) annotationPlugin
                    .createAnnotationManager(AnnotationType.PointAnnotation, null);

            // Agregar pin UGB permanente
            agregarPinUGB();

            // Configurar click listener para agregar puntos
            configurarClickMapa(style);
        });
    }

    /**
     * Agregar pin UGB en el mapa
     */
    private void agregarPinUGB() {
        mapManager.agregarPinPermanente(
                Point.fromLngLat(-88.41783453298294, 13.342296805328829),
                "UGB", "#0080ff"
        );
    }

    /**
     * Configurar listener de clicks en el mapa
     */
    private void configurarClickMapa(Style style) {
        GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
            if (modoActual == MODO_LUGAR) {
                mapManager.agregarPunto(point, style);
                tvModo.setText("Puntos: " + mapManager.obtenerCantidadPuntos() +
                        " — toca Cerrar para terminar");
                return true;
            } else if (modoActual == MODO_ESPACIO) {
                if (mapManager.puntoDentroDeLugar(point)) {
                    mapManager.agregarPunto(point, style);
                    tvModo.setText("Puntos: " + mapManager.obtenerCantidadPuntos() +
                            " — toca Cerrar para terminar");
                } else {
                    Toast.makeText(this,
                            "El punto está fuera del lugar",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    // ════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE BOTONES
    // ════════════════════════════════════════════════════════════════

    /**
     * Configurar todos los listeners de botones
     */
    private void configurarBotones() {
        configurarBtnHabilitar();
        configurarBtnLugar();
        configurarBtnEspacios();
        configurarBtnCerrar();
        configurarBtnDeshacer();
        configurarBtnFinalizar();
    }

    /**
     * Botón HABILITAR - Activar/Desactivar modo edición
     */
    private void configurarBtnHabilitar() {
        btnHabilitar.setOnClickListener(v -> {
            if (modoEdicionActivo && mapManager.isModoEdicion()) {
                // Desactivar modo edición
                desactivarModoEdicion();
            } else {
                // Activar modo edición
                activarModoEdicion();
            }
        });
    }

    /**
     * Botón LUGAR - Iniciar dibujo de lugar
     */
    private void configurarBtnLugar() {
        btnLugar.setOnClickListener(v -> {
            if (modoActual == MODO_LUGAR) {
                // Cancelar modo lugar
                cancelarModo();
            } else {
                // Activar modo lugar
                iniciarModoLugar();
            }
        });
    }

    /**
     * Botón ESPACIOS - Iniciar dibujo de espacios
     */
    private void configurarBtnEspacios() {
        btnEspacios.setOnClickListener(v -> {
            if (modoActual == MODO_ESPACIO) {
                // Cancelar modo espacios
                cancelarModo();
            } else {
                // Activar modo espacios
                iniciarModoEspacios();
            }
        });
    }

    /**
     * Botón CERRAR - Cerrar polígono (Lugar o Espacio)
     */
    private void configurarBtnCerrar() {
        btnCerrar.setOnClickListener(v -> {
            mapView.getMapboxMap().getStyle(style -> {
                if (modoActual == MODO_LUGAR) {
                    mapManager.cerrarLugar(style);
                    actualizarUIAlCerrarLugar();
                } else if (modoActual == MODO_ESPACIO) {
                    mapManager.cerrarEspacio(style);
                    tvModo.setText("Espacio guardada. Dibuja la siguiente.");
                }
            });
        });
    }

    /**
     * Botón DESHACER - Quitar último punto
     */
    private void configurarBtnDeshacer() {
        btnDeshacer.setOnClickListener(v -> {
            mapManager.deshacerPunto();
            tvModo.setText("Puntos: " + mapManager.obtenerCantidadPuntos());
        });
    }

    /**
     * Botón FINALIZAR - Terminar edición
     */
    private void configurarBtnFinalizar() {
        btnFinalizar.setOnClickListener(v -> {
            modoActual = MODO_NINGUNO;
            mapManager.limpiarVérticesTemporales();
            btnLugar.setText("Lugar");
            btnFinalizar.setVisibility(View.GONE);
            btnEspacios.setVisibility(View.GONE);
            tvModo.setText("¡Listo! Lugar y espacios guardados.");
            Toast.makeText(this, "Mapa guardado correctamente", Toast.LENGTH_LONG).show();
        });
    }

    // ════════════════════════════════════════════════════════════════
    // LÓGICA DE MODOS
    // ════════════════════════════════════════════════════════════════

    /**
     * Activar modo edición
     */
    private void activarModoEdicion() {
        modoEdicionActivo = true;
        btnHabilitar.setText("CANCELAR");
        mapManager.setModoEdicion(true);

        // Mostrar solo botón Lugar inicialmente
        btnLugar.setVisibility(View.VISIBLE);
        btnEspacios.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);

        tvModo.setText("Modo edición activado - Toca 'Lugar' para comenzar");
        Toast.makeText(this, "Modo edición activado", Toast.LENGTH_SHORT).show();
    }

    /**
     * Desactivar modo edición
     */
    private void desactivarModoEdicion() {
        modoEdicionActivo = false;
        mapManager.setModoEdicion(false);
        btnHabilitar.setText("HABILITAR");

        // Ocultar todos los botones de edición
        ocultarEdicion();

        // Salir de cualquier modo activo
        if (modoActual != MODO_NINGUNO) {
            modoActual = MODO_NINGUNO;
            btnLugar.setText("Lugar");
            mapManager.limpiarVérticesTemporales();
        }

        tvModo.setText("");
        Toast.makeText(this, "Modo edición desactivado", Toast.LENGTH_SHORT).show();
    }

    /**
     * Iniciar modo LUGAR
     */
    private void iniciarModoLugar() {
        modoActual = MODO_LUGAR;
        btnLugar.setText("Cancelar");
        btnEspacios.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.VISIBLE);
        btnDeshacer.setVisibility(View.VISIBLE);
        mapManager.limpiarVérticesTemporales();
        tvModo.setText("Dibuja el LUGAR — toca el mapa punto a punto");
    }

    /**
     * Iniciar modo ESPACIOS
     */
    private void iniciarModoEspacios() {
        modoActual = MODO_ESPACIO;
        btnEspacios.setText("Salir espacios");
        btnCerrar.setVisibility(View.VISIBLE);
        btnDeshacer.setVisibility(View.VISIBLE);
        btnFinalizar.setVisibility(View.GONE);
        mapManager.limpiarVérticesTemporales();
        tvModo.setText("Dibuja ESPACIOS dentro del Lugar " + mapManager.obtenerLugarActualId());
    }

    /**
     * Cancelar modo actual
     */
    private void cancelarModo() {
        modoActual = MODO_NINGUNO;
        btnLugar.setText("Lugar");
        btnEspacios.setText("Espacios");

        // Ocultar botones de dibujo
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);

        // 👇 IMPORTANTE: Mantener visible Finalizar si estamos en modo edición
        if (modoEdicionActivo) {
            btnFinalizar.setVisibility(View.VISIBLE);
        } else {
            btnFinalizar.setVisibility(View.GONE);
        }

        mapManager.limpiarVérticesTemporales();
        tvModo.setText("");
    }

    /**
     * Actualizar UI después de cerrar un lugar
     */
    private void actualizarUIAlCerrarLugar() {
        modoActual = MODO_NINGUNO;
        btnLugar.setText("Lugar");
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.VISIBLE);
        btnFinalizar.setVisibility(View.VISIBLE);
        tvModo.setText("Lugar guardado. Dibuja espacios o finaliza.");
    }

    // ════════════════════════════════════════════════════════════════
    // UTILIDADES DE UI
    // ════════════════════════════════════════════════════════════════

    /**
     * Ocultar todos los botones de edición
     */
    private void ocultarEdicion() {
        btnLugar.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);
    }

    /**
     * Ocultar botones de edición (Cerrar y Deshacer)
     */
    private void ocultarBotonesEdicion() {
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
    }

    /*
    // Lifecycle methods (descomentar si necesitas)
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    */
}