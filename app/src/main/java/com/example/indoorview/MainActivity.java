package com.example.indoorview;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private boolean usuarioLog;
    private int usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private int usuarioTipo;
    private String usuarioCarnet;
    private String usuarioCorreo;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Obtener los datos de sesion
        obtenerDatosSesion();

        // Cargar el mapa al iniciar
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedor_principal, new MapaFragment())
                    .commit();
        }

        // Escuchar cambios en la barra
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_mapa) {
                fragment = new MapaFragment();
            } else if (itemId == R.id.navigation_eventos) {
                fragment = new EventosFragment();
            } else if (itemId == R.id.navigation_perfil) {
                fragment = new PerfilFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.contenedor_principal, fragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    // Obtener los datos de sesion
    private void obtenerDatosSesion() {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        usuarioLog = prefs.getBoolean("isLoggedIn", true);
        usuarioId = prefs.getInt("usuario_id", -1);
        usuarioNombre = prefs.getString("usuario_nombre", "");
        usuarioApellidos = prefs.getString("usuario_apellidos", "");
        usuarioTipo = prefs.getInt("usuario_tipo", 1);
        usuarioCarnet = prefs.getString("usuario_carnet", "");
        usuarioCorreo = prefs.getString("usuario_correo", "");
    }
}