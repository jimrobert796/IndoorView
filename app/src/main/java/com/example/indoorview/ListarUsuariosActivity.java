package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorview.models.Usuarios;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Activity para listar usuarios CON PAGINACIÓN
 *
 * Características:
 * - Carga 20 usuarios por página
 * - Compatible con búsqueda y filtros
 * - Botones para navegar entre páginas
 * - Información de página actual y total
 * - Optimizado para 1000+ usuarios
 */
public class ListarUsuariosActivity extends AppCompatActivity implements UsuariosAdapter.UsuarioClickListener {

    // ==================== VISTAS ====================
    private ImageView btnRegresar;
    private EditText etBuscador;
    private Spinner spinnerFiltro;
    private RecyclerView rvUsuarios;
    private FloatingActionButton fabAgregarUsuario;
    private TextView tvPaginacion;
    private ImageView btnAnterior, btnSiguiente;

    // ==================== BASE DE DATOS ====================
    private Database db;
    private UsuariosAdapter adaptador;
    private List<Usuarios> listaUsuariosPagina;

    // ==================== PAGINACIÓN ====================
    private int paginaActual = 1;
    private final int usuariosPorPagina = 20;  // 20 usuarios por página
    private int totalUsuarios = 0;
    private int totalPaginas = 0;

    // ==================== FILTROS ====================
    private String textoBusqueda = "";
    private int tipoFiltro = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        inicializarVistas();
        configurarSpinnerFiltro();
        db = new Database(this);

        cargarUsuarios();
        configurarBotones();
        configurarBuscador();
    }

    /**
     * Inicializar vistas
     */
    private void inicializarVistas() {
        btnRegresar = findViewById(R.id.btn_regresar);
        etBuscador = findViewById(R.id.et_buscador);
        spinnerFiltro = findViewById(R.id.spinner_filtro_tipo);
        rvUsuarios = findViewById(R.id.rv_usuarios);
        fabAgregarUsuario = findViewById(R.id.fab_agregar_usuario);
        tvPaginacion = findViewById(R.id.tv_paginacion);
        btnAnterior = findViewById(R.id.btn_anterior_pagina);
        btnSiguiente = findViewById(R.id.btn_siguiente_pagina);

        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Configurar spinner de filtro
     */
    private void configurarSpinnerFiltro() {
        String[] opciones = {"Todos", "Estudiantes", "Administradores"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapter);

        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: tipoFiltro = -1; break;
                    case 1: tipoFiltro = 1; break;
                    case 2: tipoFiltro = 2; break;
                }
                paginaActual = 1; // Volver a página 1
                cargarUsuarios();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * ✅ NUEVO: Cargar usuarios CON PAGINACIÓN
     *
     * Este método:
     * 1. Obtiene el total de usuarios (con filtros)
     * 2. Calcula total de páginas
     * 3. Obtiene usuarios de la página actual
     * 4. Actualiza el adaptador
     * 5. Actualiza información de paginación
     */
    private void cargarUsuarios() {
        // Obtener total de usuarios (con filtros aplicados)
        totalUsuarios = db.getTotalUsuariosFiltrados(textoBusqueda, tipoFiltro);

        // Calcular total de páginas
        if (totalUsuarios == 0) {
            totalPaginas = 1;
        } else {
            totalPaginas = (int) Math.ceil((double) totalUsuarios / usuariosPorPagina);
        }

        // Asegurar que página actual es válida
        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }
        if (paginaActual < 1) {
            paginaActual = 1;
        }

        // Obtener usuarios de la página actual (con filtros)
        listaUsuariosPagina = db.getUsuariosPaginadosFiltrados(
                paginaActual,
                usuariosPorPagina,
                textoBusqueda,
                tipoFiltro
        );

        // Actualizar adaptador
        if (adaptador == null) {
            adaptador = new UsuariosAdapter(listaUsuariosPagina, this, this);
            rvUsuarios.setAdapter(adaptador);
        } else {
            adaptador.actualizarLista(listaUsuariosPagina);
        }

        // Actualizar información de paginación
        actualizarInfoPaginacion();
    }

    /**
     * ✅ NUEVO: Actualizar información de paginación
     *
     * Muestra:
     * - Página actual / Total de páginas
     * - Total de usuarios
     * - Estado de botones (habilitado/deshabilitado)
     */
    private void actualizarInfoPaginacion() {
        String texto = "Página " + paginaActual + " de " + totalPaginas +
                " (" + totalUsuarios + " usuarios)";
        tvPaginacion.setText(texto);

        // Habilitar/deshabilitar botones
        boolean puedeAnterior = paginaActual > 1;
        boolean puedeSiguiente = paginaActual < totalPaginas;

        btnAnterior.setEnabled(puedeAnterior);
        btnSiguiente.setEnabled(puedeSiguiente);

        // Cambiar opacidad si está deshabilitado
        btnAnterior.setAlpha(puedeAnterior ? 1.0f : 0.5f);
        btnSiguiente.setAlpha(puedeSiguiente ? 1.0f : 0.5f);
    }

    /**
     * Configurar botones
     */
    private void configurarBotones() {
        // Botón regresar
        btnRegresar.setOnClickListener(v -> finish());

        // Botón agregar usuario
        fabAgregarUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(ListarUsuariosActivity.this, AgregarUsuarioActivity.class);
            startActivityForResult(intent, 1);
        });

        // ✅ NUEVO: Botón página anterior
        btnAnterior.setOnClickListener(v -> {
            if (paginaActual > 1) {
                paginaActual--;
                cargarUsuarios();
                rvUsuarios.scrollToPosition(0); // Scroll al inicio
            }
        });

        // ✅ NUEVO: Botón página siguiente
        btnSiguiente.setOnClickListener(v -> {
            if (paginaActual < totalPaginas) {
                paginaActual++;
                cargarUsuarios();
                rvUsuarios.scrollToPosition(0); // Scroll al inicio
            }
        });
    }

    /**
     * Configurar buscador en tiempo real
     */
    private void configurarBuscador() {
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusqueda = s.toString();
                paginaActual = 1; // Volver a página 1 al buscar
                cargarUsuarios();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Callback: Editar usuario
     */
    @Override
    public void onEditarClick(Usuarios usuario) {
        Intent intent = new Intent(ListarUsuariosActivity.this, AgregarUsuarioActivity.class);
        intent.putExtra("usuario_id", usuario.getId_usuario());
        intent.putExtra("es_edicion", true);
        startActivityForResult(intent, 1);
    }

    /**
     * Callback: Eliminar usuario
     */
    @Override
    public void onEliminarClick(Usuarios usuario) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Estás seguro que deseas eliminar a " + usuario.getNombres() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.eliminarUsuario(usuario.getId_usuario());
                    Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                    recargarLista();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Recargar lista desde el inicio
     */
    private void recargarLista() {
        paginaActual = 1;
        textoBusqueda = "";
        tipoFiltro = -1;
        etBuscador.setText("");
        spinnerFiltro.setSelection(0);
        cargarUsuarios();
    }

    /**
     * Cuando regresa de AgregarUsuarioActivity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            recargarLista();
        }
    }

    /**
     * Limpiar recursos
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}