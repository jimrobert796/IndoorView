package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.indoorview.models.Usuarios;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class NuevaContraActivity extends AppCompatActivity {

    private Button btnConfirmar;
    private ImageView btnRegresar;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private FirebaseHelper firebaseHelper;
    private String correo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nueva_contrasena);

        firebaseHelper = new FirebaseHelper();
        correo = getIntent().getStringExtra("correo");

        btnConfirmar = findViewById(R.id.btn_confirmar);
        btnRegresar = findViewById(R.id.btn_regresar);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnRegresar.setOnClickListener(v -> finish());

        btnConfirmar.setOnClickListener(v -> {
            if (validarCampos()) {
                actualizarContraseña();
            }
        });
    }

    private boolean validarCampos() {
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validar que no estén vacíos
        if (password.isEmpty()) {
            etPassword.setError("La contraseña no puede estar vacía");
            etPassword.requestFocus();
            return false;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Confirme la contraseña");
            etConfirmPassword.requestFocus();
            return false;
        }

        // Validar longitud mínima
        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return false;
        }

        // Validar que las contraseñas coincidan
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void actualizarContraseña() {
        String password = etPassword.getText().toString().trim();
        btnConfirmar.setEnabled(false);

        // Hashear la contraseña
        String passwordHasheada = Utilidades.hashPassword(password);

        // Obtener el usuario por correo
        firebaseHelper.buscarUsuarioPorCorreo(correo, new FirebaseHelper.FirebaseUsuarioCallback() {
            @Override
            public void onSuccess(Usuarios result) {
                if (result instanceof Usuarios) {
                    Usuarios usuario = (Usuarios) result;
                    // Actualizar la contraseña hasheada
                    usuario.setContraseña(passwordHasheada);

                    // Guardar en Firebase
                    firebaseHelper.actualizarUsuarioPorCarnet(usuario, new FirebaseHelper.FirebaseCallback() {

                        @Override
                        public void onSuccess(String mensaje) {
                            // ✅ MOSTRAR DIALOG DE ÉXITO
                            mostrarDialogoExito();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(NuevaContraActivity.this,
                                    "Error al actualizar: " + error, Toast.LENGTH_LONG).show();
                            btnConfirmar.setEnabled(true);
                        }
                    });
                }
            }
            @Override
            public void onNotFound() {
                Toast.makeText(
                        NuevaContraActivity.this,
                        "Correo no encontrado",
                        Toast.LENGTH_SHORT
                ).show();
                btnConfirmar.setEnabled(true);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(NuevaContraActivity.this,
                        "Error: " + error, Toast.LENGTH_LONG).show();
                btnConfirmar.setEnabled(true);
            }
        });
    }

    /**
     * Mostrar dialog personalizado de éxito
     */
    private void mostrarDialogoExito() {
        // Crear el dialog con tu layout personalizado
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflar el layout personalizado
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_password_reset_success, null);

        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        // HACER EL FONDO TRANSPARENTE
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Obtener referencias a los elementos del dialog
        Button btnVamos = dialogView.findViewById(R.id.btn_vamos);

        btnVamos.setOnClickListener(v -> {
            android.content.SharedPreferences prefs =
                    getSharedPreferences("reset_password", MODE_PRIVATE);
            prefs.edit().clear().apply();

            dialog.dismiss();

            Intent intent = new Intent(NuevaContraActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }
}