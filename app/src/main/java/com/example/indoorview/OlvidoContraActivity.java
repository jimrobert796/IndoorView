package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorview.models.Usuarios;

public class OlvidoContraActivity extends AppCompatActivity {

    private Button btnConfirmar;
    private ImageView btnRegresar;
    private EditText etCorreo;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olvido_contra);

        firebaseHelper = new FirebaseHelper();

        btnConfirmar = findViewById(R.id.btn_confirmar);
        btnRegresar = findViewById(R.id.btn_regresar);
        etCorreo = findViewById(R.id.et_correo);

        btnRegresar.setOnClickListener(v -> finish());

        btnConfirmar.setOnClickListener(v -> {
            if (validarCampos()) {
                verificarCorreoYEnviarCodigo();
            }
        });
    }

    private boolean validarCampos() {
        String correo = etCorreo.getText().toString().trim();

        // Validar formato de correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Ingrese un correo electrónico válido");
            etCorreo.requestFocus();
            return false;
        }

        // Validar dominio institucional (opcional - solo advertencia)
        if (!correo.endsWith("@ugb.edu.sv")) {
            Toast.makeText(this, "Sugerencia: Use correo institucional @ugb.edu.sv", Toast.LENGTH_LONG).show();
        }

        return true;
    }

    private void verificarCorreoYEnviarCodigo() {
        String correo = etCorreo.getText().toString().trim();
        btnConfirmar.setEnabled(false);

        // Buscar el usuario por correo en Firebase
        firebaseHelper.buscarUsuarioPorCorreo(correo, new FirebaseHelper.FirebaseUsuarioCallback() {
            @Override
            public void onSuccess(Usuarios usuario) {
                if (usuario != null) {
                    // El correo existe, generar código
                    String codigoVerificacion = generarCodigoAleatorio();

                    // Guardar el código temporalmente (en SharedPreferences o Intent)
                    guardarCodigoTemporal(codigoVerificacion, correo);

                    // Enviar correo con el código
                    EmailHelper.enviarCodigoVerificacion(correo, codigoVerificacion);

                    Toast.makeText(OlvidoContraActivity.this,
                            "Código enviado a " + correo, Toast.LENGTH_LONG).show();

                    // Ir a la siguiente activity
                    Intent intent = new Intent(OlvidoContraActivity.this, CodigoVerificacionActivity.class);
                    intent.putExtra("correo", correo);
                    intent.putExtra("codigo", codigoVerificacion);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(OlvidoContraActivity.this,
                            "El correo no está registrado", Toast.LENGTH_LONG).show();
                    btnConfirmar.setEnabled(true);
                }
            }

            @Override
            public void onNotFound() {
                Toast.makeText(
                        OlvidoContraActivity.this,
                        "Correo no encontrado",
                        Toast.LENGTH_SHORT
                ).show();
                btnConfirmar.setEnabled(true);      // ← AGREGADO
                btnConfirmar.setAlpha(1.0f);
            }

            // DESPUÉS (✅ CORRECTO)
            @Override
            public void onError(String error) {
                Toast.makeText(OlvidoContraActivity.this,
                        "Error: " + error, Toast.LENGTH_LONG).show();
                btnConfirmar.setEnabled(true);      // ← AGREGADO
                btnConfirmar.setAlpha(1.0f);        // ← AGREGADO

                Log.e("OlvidoContra", "Error al buscar correo: " + error);
            }
        });
    }

    private String generarCodigoAleatorio() {
        // Generar código de 4 dígitos aleatorios
        return String.format("%04d", (int) (Math.random() * 10000));
    }

    private void guardarCodigoTemporal(String codigo, String correo) {
        // Guardar en SharedPreferences para acceso rápido
        android.content.SharedPreferences prefs = getSharedPreferences("reset_password", MODE_PRIVATE);
        prefs.edit()
                .putString("codigo_temporal", codigo)
                .putString("correo_temporal", correo)
                .putLong("tiempo_expiracion", System.currentTimeMillis() + (5 * 60 * 1000)) // 5 minutos
                .apply();
    }
}