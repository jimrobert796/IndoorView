package com.example.indoorview;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    private FirebaseHelper firebaseHelper;
    private  DetectarInternet detectarInternet;

    // En ListarUsuariosActivity.java

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        inicializarVistas();
        configurarSpinnerFiltro();

        db = new Database(this);
        firebaseHelper = new FirebaseHelper();
        detectarInternet = new DetectarInternet(this);

        configurarBotones();
        configurarBuscador();

        // 1️⃣ Primero mostrar datos locales (si hay)
        cargarUsuarios();

        // 2️⃣ Luego sincronizar con Firebase en segundo plano esto sigue
        sincronizarConFirebase();
    }

    /**
     * Sincronizar todos los usuarios desde Firebase a SQLite
     */
    private void sincronizarConFirebase() {
        // Verificar si hay internet
        if (!detectarInternet.hayConexionInternet()) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso (opcional)
        Toast.makeText(this, "Sincronizando usuarios...", Toast.LENGTH_SHORT).show();

        // Llamar al método de Firebase
        firebaseHelper.obtenerYGuardarTodosLosUsuarios(this);

        new android.os.Handler().postDelayed(() -> {
            cargarUsuarios();
            Toast.makeText(this, "Sincronización completada", Toast.LENGTH_SHORT).show();
        }, 3000);


    }


    private void sincronizarConBusquedaFirebase(
            String textoBusqueda,
            int tipoFiltro
    ) {

        if (!detectarInternet.hayConexionInternet()) {

            Toast.makeText(
                    this,
                    "Sin conexión a internet",
                    Toast.LENGTH_SHORT
            ).show();
            cargarUsuarios();
            return;
        }

        Toast.makeText(
                this,
                "Buscando usuarios...",
                Toast.LENGTH_SHORT
        ).show();

        firebaseHelper.buscarYGuardarUsuarios(
                textoBusqueda,
                tipoFiltro,
                this,
                new FirebaseHelper.FirebaseBusquedaCallback() {

                    @Override
                    public void onComplete(int resultados, int guardados) {

                        runOnUiThread(() -> {

                            Log.d("BUSQUEDA_FIREBASE",
                                    "✅ Resultados: " + resultados);

                            Log.d("BUSQUEDA_FIREBASE",
                                    "💾 Guardados: " + guardados);

                            // Recargar RecyclerView
                            cargarUsuarios();

                            Toast.makeText(
                                    ListarUsuariosActivity.this,
                                    "Usuarios encontrados: " + resultados,
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> {

                            Log.e("BUSQUEDA_FIREBASE",
                                    "❌ Error: " + error);

                            Toast.makeText(
                                    ListarUsuariosActivity.this,
                                    "Error: " + error,
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }
                }
        );
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
                sincronizarConBusquedaFirebase(textoBusqueda,tipoFiltro);
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
        mostrarDialogoConfirmacion(
                "Eliminar Usuario",
                "¿Estás seguro que deseas eliminar a " + usuario.getNombres() + "?",
                "Eliminar",
                ()->{
                    eliminarUsuario(usuario);
                }
                );
    }


    private void eliminarUsuario(Usuarios usuario){

        if (detectarInternet.hayConexionInternet()){

            db.eliminarUsuario(usuario.getId_usuario());
            Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
            recargarLista();

            // Guardar en Firebase
            firebaseHelper.softDeleteUsuarioPorCarnet(usuario.getCarnet(), new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {
                    Log.d("EVENTO", "✅ " + mensaje);
                }

                @Override
                public void onError(String error) {
                    Log.e("EVENTO", "❌ " + error);
                }
            });
        }else {
            // Guardando sin conexion con estado 4 eliminado sin conexion
            usuario.setEstado(4); // Eliminado sin conexion

            db.actualizarUsuario(usuario);
            Toast.makeText(this, "Usuario eliminado sin conexion", Toast.LENGTH_SHORT).show();
            recargarLista();
        }



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

    public void mostrarDialogoConfirmacion(
            String titulo,
            String mensaje,
            String textoPositivo,
            Runnable onConfirm
    ) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton(textoPositivo, (d, w) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();

        // Cambiar color del botón positivo a #2196F3
        Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(Color.parseColor("#2196F3"));

        // Opcional: También cambiar el botón negativo si quieres
        Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setTextColor(Color.parseColor("#2196F3"));
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