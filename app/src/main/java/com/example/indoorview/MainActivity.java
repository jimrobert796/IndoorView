package com.example.indoorview;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.maps.CameraOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Display de mapa
        mapView = findViewById(R.id.mapView);

        // Estilo de mapa
        mapView.getMapboxMap().loadStyleUri(Style.OUTDOORS, style -> {

            // Centrar en Universidad Gerardo Barrios
            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            // Coordenadas de la u
                            .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                            .zoom(16.0)  // 16 = nivel de campus universitario
                            .build()
            );

        });
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
}