package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.indoorview.models.Usuarios;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ListarUsuariosActivity extends AppCompatActivity {

    // Vistas
    private ImageView btnRegresar;
    private EditText etBuscador;
    private LinearLayout llContenedorUsuarios;
    private FloatingActionButton fab_agregar_usuario;

    // Base de datos
    private Database db;

    // Lista de usuarios
    private List<Usuarios> listaUsuariosOriginal;
    private List<Usuarios> listaUsuariosFiltrada;

    // Variables de sesión (para saber si es admin)
    private boolean esAdmin = false; // Cambia según tu lógica de login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        fab_agregar_usuario = findViewById(R.id.fab_agregar_usuario);

        fab_agregar_usuario.setOnClickListener(v -> {
            Intent intent = new Intent(ListarUsuariosActivity.this, AgregarUsuarioActivity.class);
            startActivity(intent);
        });


    }
}