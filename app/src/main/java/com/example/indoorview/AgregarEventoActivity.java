package com.example.indoorview;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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
import androidx.cardview.widget.CardView;

import com.example.indoorview.models.Eventos;

import java.util.Calendar;

public class AgregarEventoActivity extends AppCompatActivity {

    // Componentes del layout
    private ImageView btnRegresar;
    private TextView tvTituloFormulario;
    private TextView tvModoEdicion;
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

    // Variables para almacenar fechas y horas (SEPARADAS)
    private Calendar calendarFechaInicio = Calendar.getInstance();
    private Calendar calendarFechaFin = Calendar.getInstance();
    private Calendar calendarHoraInicio = Calendar.getInstance();
    private Calendar calendarHoraFin = Calendar.getInstance();

    // Variables de edición
    private boolean esEdicion = false;
    private int idEventoEditando = -1;
    private String nombreEventoEditando = "";
    private Database bdEventos;
    private int idLugar = -1;
    private int idEspacio = -1;
    private String latitud = "";
    private String longitud = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_evento);

        // Inicializar BD
        bdEventos = new Database(this);

        // Inicializar vistas
        initViews();

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
     * Verificar si viene de edición y cargar datos
     */
    private void verificarIntentExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            esEdicion = extras.getBoolean("es_edicion", false);
            if (esEdicion) {
                idEventoEditando = extras.getInt("id_evento", -1);
                idLugar = extras.getInt("id_lugar", -1);
                idEspacio = extras.getInt("id_espacio", -1);
                latitud = extras.getString("latitud", "");
                longitud = extras.getString("longitud", "");
                nombreEventoEditando = extras.getString("nombre", "");

                // ===== MEJORADO: Cargar fecha y hora separadas =====
                etTituloEvento.setText(extras.getString("nombre", ""));
                etDescripcion.setText(extras.getString("descripcion", ""));

                // Cargar fechas y horas separadas
                etFechaInicio.setText(extras.getString("fecha_inicio", ""));
                etHoraInicio.setText(extras.getString("hora_inicio", ""));
                etFechaFin.setText(extras.getString("fecha_fin", ""));
                etHoraFin.setText(extras.getString("hora_fin", ""));

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

        // Botón agregar punto de referencia
        btnAgregarPunto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AgregarEventoActivity.this,
                        "Funcionalidad de agregar punto en desarrollo", Toast.LENGTH_SHORT).show();
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

        // ===== MEJORADO: Guardar fecha y hora SEPARADAS =====
        if (esEdicion) {
            // EDITAR evento existente
            Eventos evento = new Eventos(
                    idEventoEditando,
                    idLugar != -1 ? idLugar : 1,
                    idEspacio != -1 ? idEspacio : 1,
                    titulo,
                    descripcion,
                    latitud.isEmpty() ? "0.0" : latitud,
                    longitud.isEmpty() ? "0.0" : longitud,
                    fechaInicio,      // dd/mm/yyyy
                    horaInicio,        // hh:mm
                    fechaFin,          // dd/mm/yyyy
                    horaFin,           // hh:mm
                    1 // estado activo
            );

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
                    0, // id será generado por la BD
                    idLugar != -1 ? idLugar : 1,
                    idEspacio != -1 ? idEspacio : 1,
                    titulo,
                    descripcion,
                    latitud.isEmpty() ? "0.0" : latitud,
                    longitud.isEmpty() ? "0.0" : longitud,
                    fechaInicio,      // dd/mm/yyyy
                    horaInicio,        // hh:mm
                    fechaFin,          // dd/mm/yyyy
                    horaFin,           // hh:mm
                    1 // estado activo
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