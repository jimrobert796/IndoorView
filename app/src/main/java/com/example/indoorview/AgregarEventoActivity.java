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
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.Calendar;

public class AgregarEventoActivity extends AppCompatActivity {

    // Componentes del layout
    private ImageView btnRegresar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_evento);

        // Inicializar vistas
        initViews();

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        btnRegresar = findViewById(R.id.btn_regresar);
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
                // Aquí puedes implementar la selección de punto en el mapa
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
                            // Validar que fecha fin no sea menor que fecha inicio
                            if (calendarFechaInicio.after(Calendar.getInstance())) {
                                calendarFechaInicio.set(year, month, dayOfMonth);
                            }
                            if (calendarFechaInicio.getTimeInMillis() >
                                    Calendar.getInstance().getTimeInMillis()) {
                                Toast.makeText(AgregarEventoActivity.this,
                                        "La fecha fin no puede ser menor a la fecha inicio",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
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
        // Obtener datos
        String titulo = etTituloEvento.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();
        String horaInicio = etHoraInicio.getText().toString().trim();
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

        if (fechaFin.isEmpty()) {
            Toast.makeText(this, "Seleccione la fecha de fin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (horaInicio.isEmpty()) {
            Toast.makeText(this, "Seleccione la hora de inicio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (horaFin.isEmpty()) {
            Toast.makeText(this, "Seleccione la hora de fin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aquí puedes guardar el evento en tu base de datos o enviarlo de vuelta
        String mensaje = "Evento creado exitosamente:\n\n" +
                "Título: " + titulo + "\n" +
                "Descripción: " + descripcion + "\n" +
                "Fecha inicio: " + fechaInicio + " " + horaInicio + "\n" +
                "Fecha fin: " + fechaFin + " " + horaFin;

        Toast.makeText(this, "Evento guardado correctamente", Toast.LENGTH_LONG).show();

        // Enviar resultado de vuelta si es necesario
        /*
        Intent resultIntent = new Intent();
        resultIntent.putExtra("titulo", titulo);
        resultIntent.putExtra("descripcion", descripcion);
        resultIntent.putExtra("fecha_inicio", fechaInicio);
        resultIntent.putExtra("fecha_fin", fechaFin);
        resultIntent.putExtra("hora_inicio", horaInicio);
        resultIntent.putExtra("hora_fin", horaFin);
        setResult(RESULT_OK, resultIntent);
        */

        // Cerrar la actividad
        finish();
    }
}