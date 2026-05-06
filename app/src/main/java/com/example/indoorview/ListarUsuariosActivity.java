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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorview.models.Usuarios;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ListarUsuariosActivity extends AppCompatActivity implements UsuariosAdapter.UsuarioClickListener {

    private ImageView btnRegresar;
    private EditText etBuscador;
    private Spinner spinnerFiltro;
    private RecyclerView rvUsuarios;
    private FloatingActionButton fabAgregarUsuario;

    private Database db;
    private UsuariosAdapter adaptador;
    private List<Usuarios> listaUsuariosOriginal;
    private List<Usuarios> listaUsuariosFiltrada;

    private String textoBusqueda = "";  // Guardar texto de búsqueda
    private int tipoFiltro = -1;  // -1 = todos, 1 = estudiante, 2 = admin

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        inicializarVistas();

        // Configurar el spinner con opciones de filtro
        configurarSpinnerFiltro();

        db = new Database(this);
        cargarUsuarios();
        configurarBotones();
        configurarBuscador();
    }

    private void inicializarVistas() {
        btnRegresar = findViewById(R.id.btn_regresar);
        etBuscador = findViewById(R.id.et_buscador);
        spinnerFiltro = findViewById(R.id.spinner_filtro_tipo);
        rvUsuarios = findViewById(R.id.rv_usuarios);
        fabAgregarUsuario = findViewById(R.id.fab_agregar_usuario);

        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Configurar el spinner de filtro
     */
    private void configurarSpinnerFiltro() {
        // Opciones del filtro
        String[] opciones = {"Todos", "Estudiantes", "Administradores"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapter);

        // Listener para cuando cambia el filtro
        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Actualizar tipo de filtro según la posición seleccionada
                switch (position) {
                    case 0: // Todos
                        tipoFiltro = -1;
                        break;
                    case 1: // Estudiantes
                        tipoFiltro = 1;
                        break;
                    case 2: // Administradores
                        tipoFiltro = 2;
                        break;
                }
                aplicarFiltros(); // Aplicar ambos filtros (texto + tipo)
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void cargarUsuarios() {
        listaUsuariosOriginal = db.getUsuarios();
        listaUsuariosFiltrada = new ArrayList<>(listaUsuariosOriginal);
        adaptador = new UsuariosAdapter(listaUsuariosFiltrada, this, this);
        rvUsuarios.setAdapter(adaptador);
    }

    private void configurarBotones() {
        btnRegresar.setOnClickListener(v -> finish());
        fabAgregarUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(ListarUsuariosActivity.this, AgregarUsuarioActivity.class);
            startActivityForResult(intent, 1);
        });
    }

    private void configurarBuscador() {
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusqueda = s.toString();
                aplicarFiltros(); // Aplicar ambos filtros
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Aplicar filtros combinados: texto + tipo de usuario
     */
    private void aplicarFiltros() {
        listaUsuariosFiltrada.clear();

        for (Usuarios usuario : listaUsuariosOriginal) {
            boolean coincideTexto = true;
            boolean coincideTipo = true;

            // Filtrar por texto (nombre o carnet)
            if (!textoBusqueda.isEmpty()) {
                String busqueda = textoBusqueda.toLowerCase();
                coincideTexto = usuario.getNombres().toLowerCase().contains(busqueda) ||
                        usuario.getCarnet().toLowerCase().contains(busqueda);
            }

            // Filtrar por tipo de usuario
            if (tipoFiltro != -1) {
                coincideTipo = usuario.getId_tipo() == tipoFiltro;
            }

            // Si cumple ambos filtros, agregar a la lista
            if (coincideTexto && coincideTipo) {
                listaUsuariosFiltrada.add(usuario);
            }
        }

        adaptador.notifyDataSetChanged();
    }

    @Override
    public void onEditarClick(Usuarios usuario) {
        Intent intent = new Intent(ListarUsuariosActivity.this, AgregarUsuarioActivity.class);
        intent.putExtra("usuario_id", usuario.getId_usuario());
        intent.putExtra("es_edicion", true);
        startActivityForResult(intent, 1);
    }

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

    private void recargarLista() {
        cargarUsuarios();
        etBuscador.setText("");
        spinnerFiltro.setSelection(0); // Resetear filtro a "Todos"
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            recargarLista();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}