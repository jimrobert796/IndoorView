package com.example.indoorview;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.indoorview.models.Eventos;

import java.util.Calendar;

public class AgregarEventoActivity extends AppCompatActivity {

    // Componentes del layout
    private ImageView btnRegresar;
    private TextView tvTituloFormulario;
    private TextView tvModoEdicion;
    private TextView tvUbicacionSeleccionada;
    private EditText etTituloEvento;
    private EditText etDescripcion;
    private Button btnAgregarPunto;
    private EditText etFechaInicio;
    private EditText etFechaFin;
    private EditText etHoraInicio;
    private EditText etHoraFin;
    private Button btnGuardarEvento;

    // Botones de selección
    private ImageButton btnFechaInicio;
    private ImageButton btnFechaFin;
    private ImageButton btnHoraInicio;
    private ImageButton btnHoraFin;

    // Variables para almacenar fechas y horas
    private Calendar calendarFechaInicio = Calendar.getInstance();
    private Calendar calendarFechaFin = Calendar.getInstance();
    private Calendar calendarHoraInicio = Calendar.getInstance();
    private Calendar calendarHoraFin = Calendar.getInstance();

    // Variables de edición
    private boolean esEdicion = false;
    private int idEventoEditando = -1;
    private String nombreEventoEditando = "";
    private Database bdEventos;
    private String latitud = "0.0";
    private String longitud = "0.0";

    // ===== LAUNCHER PARA MAPA EN MODO SELECCIÓN
    private ActivityResultLauncher<Intent> mapaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_evento);

        // Inicializar BD
        bdEventos = new Database(this);

        // Inicializar vistas
        initViews();

        // ===== INICIALIZAR LAUNCHER PARA MAPA
        inicializarMapaLauncher();

        // Verificar si es edición o creación
        verificarIntentExtras();

        // Actualizar título dinámicamente
        actualizarTitulo();

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        btnRegresar = findViewById(R.id.btn_regresar);
        tvTituloFormulario = findViewById(R.id.tv_titulo_formulario);

        try {
            tvModoEdicion = findViewById(R.id.tvModo);
        } catch (Exception e) {
            tvModoEdicion = null;
        }
        /*
        // ===== INICIALIZAR TEXTVIEW DE UBICACIÓN (si existe en el layout)
        try {
            tvUbicacionSeleccionada = findViewById(R.id.tv_ubicacion_seleccionada);
        } catch (Exception e) {
            tvUbicacionSeleccionada = null;
        }

         */

        etTituloEvento = findViewById(R.id.et_titulo_evento);
        etDescripcion = findViewById(R.id.et_descripcion);
        btnAgregarPunto = findViewById(R.id.btn_agregar_punto);
        etFechaInicio = findViewById(R.id.et_fecha_inicio);
        etFechaFin = findViewById(R.id.et_fecha_fin);
        etHoraInicio = findViewById(R.id.et_hora_inicio);
        etHoraFin = findViewById(R.id.et_hora_fin);
        btnGuardarEvento = findViewById(R.id.btn_guardar_evento);
        btnFechaInicio = findViewById(R.id.btn_fecha_inicio);
        btnFechaFin = findViewById(R.id.btn_fecha_fin);
        btnHoraInicio = findViewById(R.id.btn_hora_inicio);
        btnHoraFin = findViewById(R.id.btn_hora_fin);
    }

    /**
     * ===== INICIALIZAR LAUNCHER PARA MAPA
     */
    private void inicializarMapaLauncher() {
        mapaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Recuperar las coordenadas seleccionadas
                        latitud = result.getData().getStringExtra("latitud");
                        longitud = result.getData().getStringExtra("longitud");

                        // Actualizar UI
                        actualizarUbicacionUI(latitud, longitud);

                        Toast.makeText(this, "✓ Ubicación guardada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "⚠️ Selección cancelada", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * ===== ACTUALIZAR UI CON UBICACIÓN SELECCIONADA
     */
    private void actualizarUbicacionUI(String lat, String lng) {
        if (tvUbicacionSeleccionada != null) {
            tvUbicacionSeleccionada.setText("📍 Ubicación: " + lat + ", " + lng);
            tvUbicacionSeleccionada.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Verificar si viene de edición y cargar datos
     */
    private void verificarIntentExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            esEdicion = extras.getBoolean("es_edicion", false);
            if (esEdicion) {
                idEventoEditando = extras.getInt("id_evento", -1);
                latitud = extras.getString("latitud", "0.0");
                longitud = extras.getString("longitud", "0.0");
                nombreEventoEditando = extras.getString("nombre", "");

                // Cargar datos en los campos
                etTituloEvento.setText(extras.getString("nombre", ""));
                etDescripcion.setText(extras.getString("descripcion", ""));

                // Cargar fechas y horas separadas
                etFechaInicio.setText(extras.getString("fecha_inicio", ""));
                etHoraInicio.setText(extras.getString("hora_inicio", ""));
                etFechaFin.setText(extras.getString("fecha_fin", ""));
                etHoraFin.setText(extras.getString("hora_fin", ""));

                // Actualizar ubicación en UI
                if (!latitud.equals("0.0") && !longitud.equals("0.0")) {
                    actualizarUbicacionUI(latitud, longitud);
                }

                // Cambiar texto del botón guardar
                btnGuardarEvento.setText("Actualizar Evento");
            }
        }
    }

    /**
     * Actualizar el título del formulario dinámicamente
     */
    private void actualizarTitulo() {
        if (esEdicion) {
            tvTituloFormulario.setText("Editar Evento");
            tvTituloFormulario.setTextColor(getResources().getColor(android.R.color.holo_orange_light, null));

            btnAgregarPunto.setText("Cambiar punto");

            if (tvModoEdicion != null) {
                tvModoEdicion.setVisibility(View.VISIBLE);
                tvModoEdicion.setText("✎ Editando: " + nombreEventoEditando);
                tvModoEdicion.setTextColor(getResources().getColor(android.R.color.holo_orange_light, null));
            }
        } else {
            tvTituloFormulario.setText("Agregar Evento");
            tvTituloFormulario.setTextColor(getResources().getColor(android.R.color.holo_green_light, null));

            if (tvModoEdicion != null) {
                tvModoEdicion.setVisibility(View.GONE);
            }
        }
    }

    private void setupListeners() {
        // Botón regresar
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // ===== BOTÓN AGREGAR PUNTO - ABRE MAPA EN MODO SELECCIÓN
        btnAgregarPunto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirMapaSeleccionUbicacion();
            }
        });

        // Selección de fecha inicio
        btnFechaInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(true);
            }
        });

        etFechaInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(true);
            }
        });

        // Selección de fecha fin
        btnFechaFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(false);
            }
        });

        etFechaFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(false);
            }
        });

        // Selección de hora inicio
        btnHoraInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(true);
            }
        });

        etHoraInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(true);
            }
        });

        // Selección de hora fin
        btnHoraFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(false);
            }
        });

        etHoraFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(false);
            }
        });

        // Botón guardar evento
        btnGuardarEvento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarEvento();
            }
        });
    }

    /**
     * ===== ABRIR MAPA EN MODO SELECCIÓN DE UBICACIÓN
     */
    private void abrirMapaSeleccionUbicacion() {
        Intent intent = new Intent(this, MapaEventoActivity.class);
        intent.putExtra("modo_seleccion", true);  // Activar modo selección

        // Enviar nombre del evento (si existe)
        String nombre = etTituloEvento.getText().toString().trim();
        if (!nombre.isEmpty()) {
            intent.putExtra("nombre_evento", nombre);
        }

        // Enviar coordenadas actuales (si existen)
        if (latitud != null && !latitud.isEmpty() && !latitud.equals("0.0")) {
            intent.putExtra("latitud", latitud);
        } else {
            intent.putExtra("latitud", "");
        }

        if (longitud != null && !longitud.isEmpty() && !longitud.equals("0.0")) {
            intent.putExtra("longitud", longitud);
        } else {
            intent.putExtra("longitud", "");
        }

        // Opcional: enviar modo edición si aplica
        intent.putExtra("es_edicion", esEdicion);

        mapaLauncher.launch(intent);
    }

    private void showDatePicker(final boolean isFechaInicio) {
        Calendar calendar = isFechaInicio ? calendarFechaInicio : calendarFechaFin;
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AgregarEventoActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String fecha = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                        if (isFechaInicio) {
                            etFechaInicio.setText(fecha);
                            calendarFechaInicio.set(year, month, dayOfMonth);
                            // Validar que fecha fin no sea menor que fecha inicio
                            if (calendarFechaFin.before(calendarFechaInicio)) {
                                calendarFechaFin.set(year, month, dayOfMonth);
                                etFechaFin.setText(fecha);
                            }
                        } else {
                            etFechaFin.setText(fecha);
                            calendarFechaFin.set(year, month, dayOfMonth);
                        }
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void showTimePicker(final boolean isHoraInicio) {
        Calendar calendar = isHoraInicio ? calendarHoraInicio : calendarHoraFin;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                AgregarEventoActivity.this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String hora = String.format("%02d:%02d", hourOfDay, minute);
                        if (isHoraInicio) {
                            etHoraInicio.setText(hora);
                            calendarHoraInicio.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendarHoraInicio.set(Calendar.MINUTE, minute);

                            // Si la hora inicio es mayor que hora fin, sugerir actualizar hora fin
                            if (calendarHoraInicio.getTimeInMillis() > calendarHoraFin.getTimeInMillis() &&
                                    etFechaInicio.getText().toString().equals(etFechaFin.getText().toString())) {
                                Toast.makeText(AgregarEventoActivity.this,
                                        "La hora inicio no puede ser mayor que la hora fin para la misma fecha",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            etHoraFin.setText(hora);
                            calendarHoraFin.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendarHoraFin.set(Calendar.MINUTE, minute);
                        }
                    }
                },
                hour, minute, true
        );

        timePickerDialog.show();
    }

    private void guardarEvento() {
        // Obtener datos (SEPARADOS)
        String titulo = etTituloEvento.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String horaInicio = etHoraInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();
        String horaFin = etHoraFin.getText().toString().trim();

        // Validaciones
        if (titulo.isEmpty()) {
            etTituloEvento.setError("El título es requerido");
            etTituloEvento.requestFocus();
            return;
        }

        if (fechaInicio.isEmpty()) {
            Toast.makeText(this, "Seleccione la fecha de inicio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (horaInicio.isEmpty()) {
            Toast.makeText(this, "Seleccione la hora de inicio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fechaFin.isEmpty()) {
            Toast.makeText(this, "Seleccione la fecha de fin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (horaFin.isEmpty()) {
            Toast.makeText(this, "Seleccione la hora de fin", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== GUARDAR FECHA Y HORA SEPARADAS =====
        if (esEdicion) {
            // EDITAR evento existente
            Eventos evento = new Eventos(
                    idEventoEditando,
                    titulo,
                    descripcion,
                    longitud,
                    latitud,
                    fechaInicio,      // dd/mm/yyyy
                    horaInicio,        // hh:mm
                    fechaFin,          // dd/mm/yyyy
                    horaFin,           // hh:mm
                    1                  // estado activo
            );

            // ===== LOGS DE PRUEBA - VALORES OBTENIDOS =====
            Log.d("GUARDAR_EVENTO", "═══════════════════════════════════════");
            Log.d("GUARDAR_EVENTO", " Título: " + titulo);
            Log.d("GUARDAR_EVENTO", " Descripción: " + descripcion);
            Log.d("GUARDAR_EVENTO", " Fecha Inicio: " + fechaInicio);
            Log.d("GUARDAR_EVENTO", " Hora Inicio: " + horaInicio);
            Log.d("GUARDAR_EVENTO", " Fecha Fin: " + fechaFin);
            Log.d("GUARDAR_EVENTO", " Hora Fin: " + horaFin);
            Log.d("GUARDAR_EVENTO", " Latitud: " + latitud);
            Log.d("GUARDAR_EVENTO", " Longitud: " + longitud);
            Log.d("GUARDAR_EVENTO", " Modo Edición: " + esEdicion);
            Log.d("GUARDAR_EVENTO", "═══════════════════════════════════════");

            int filasActualizadas = bdEventos.updateEvento(evento);
            if (filasActualizadas > 0) {
                Toast.makeText(this, "✓ Evento actualizado correctamente", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "✗ Error al actualizar el evento", Toast.LENGTH_SHORT).show();
            }
        } else {
            // CREAR nuevo evento
            Eventos evento = new Eventos(
                    0,                 // id será generado por la BD
                    titulo,
                    descripcion,
                    longitud,
                    latitud,
                    fechaInicio,      // dd/mm/yyyy
                    horaInicio,        // hh:mm
                    fechaFin,          // dd/mm/yyyy
                    horaFin,           // hh:mm
                    1                  // estado activo
            );

            long id = bdEventos.insertarEvento(evento);
            if (id > 0) {
                Toast.makeText(this, "✓ Evento creado correctamente", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "✗ Error al crear el evento", Toast.LENGTH_SHORT).show();
            }
        }
    }
}