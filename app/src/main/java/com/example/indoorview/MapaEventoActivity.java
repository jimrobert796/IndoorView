package com.example.indoorview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraBoundsOptions;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

public class MapaEventoActivity extends AppCompatActivity {
    private MapView mapView;
    private MapManager mapManager;
    private Database db;
    private Spinner spinnerPisos;
    private ActivityResultLauncher<Intent> camaraLauncher;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_evento);

        // 1. Inicializar vistas
        mapView = findViewById(R.id.mapView);
        spinnerPisos = findViewById(R.id.spnPisos); // Asegúrate que exista en el XML

        // 2. Inicializar base de datos
        db = new Database(this);

        // 3. Inicializar launchers PRIMERO
        inicializarLaunchers();

        // 4. Configurar el mapa (esto creará mapManager internamente)
        configurarMapa();
    }

    private void inicializarLaunchers() {
        // Launcher para la cámara
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

        // Launcher para la galería
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

        // ✅ IMPORTANTE: Si mapManager ya existe, actualizar sus launchers
        if (mapManager != null) {
            // mapManager.setLaunchers(camaraLauncher, galeriaLauncher);
        }
    }

    private void configurarMapa() {
        if (mapView == null) {
            Log.e("MAPA_ERROR", "mapView es null");
            return;
        }

        try {
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

                // Desactivar edificios por defecto
                style.setStyleLayerProperty("building", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("building-extrusion", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("landuse", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("landuse-overlay", "visibility", Value.valueOf("none"));
                style.setStyleLayerProperty("pitch-outline", "visibility", Value.valueOf("none"));

                // ✅ CREAR MapManager AQUÍ, después de que todo está listo
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

                // ✅ Ahora sí, llamar a los métodos
                // Verificar que mapManager no sea null antes de usarlo
                if (mapManager != null) {
                    mapManager.cargarLineStringDetalles();
                    mapManager.cargarPoligonosLugar(); // Descomentar cuando esté listo

                    // NECESITAMOS ALGO QUE ELIMINE LOS PINES O QUITE SU FUNCIONALIDAD DE PIN
                    mapManager.ocultarPinesLugaresZoom();
                } else {
                    Toast.makeText(this, "Error: MapManager no inicializado", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("MAPA_ERROR", "Error configurando mapa: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Ciclo de vida del MapView
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