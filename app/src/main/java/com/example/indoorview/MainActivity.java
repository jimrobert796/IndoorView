package com.example.indoorview;

import android.app.Activity;
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

    //private PermissionManager permissionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Obtener los datos de sesion
        obtenerDatosSesion();


        // Se deja para pruebas futuras
        /*
        permissionManager = PermissionManager.getInstance();


        permissionManager.requestAllPermissions(this, new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionGranted(int requestCode) {}

            @Override
            public void onPermissionDenied(int requestCode) {}

            @Override
            public void onAllPermissionsGranted() {
                //Toast.makeText(LoginActivity.this, "Todos los permisos concedidos ✓", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSomePermissionsDenied(String[] permissions, int[] grantResults) {
                //Toast.makeText(getActivity(), "Algunos permisos fueron denegados", Toast.LENGTH_SHORT).show();
            }
        });

         */

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance().handlePermissionsResult(requestCode, permissions, grantResults);
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