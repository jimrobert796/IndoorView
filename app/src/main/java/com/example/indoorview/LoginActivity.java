package com.example.indoorview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorview.models.Usuarios;

public class LoginActivity extends AppCompatActivity {

    private Button btnLogin;
    private TextView olvidoContra;
    private TextView registro;
    private EditText etCarnet, etPassword;
    private Database db;
    private FirebaseHelper firebaseHelper;
    private DetectarInternet detectarInternet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Inicializar vistas
        btnLogin = findViewById(R.id.btn_login);
        olvidoContra = findViewById(R.id.tv_forgot_password);
        registro = findViewById(R.id.tv_registrate);
        etCarnet = findViewById(R.id.input_carnet);
        etPassword = findViewById(R.id.input_password);

        // Inicializar base de datos
        db = new Database(this);
        firebaseHelper = new FirebaseHelper();
        detectarInternet = new DetectarInternet(this);

        // Verificar si ya hay sesión activa
        verificarSesion();

        btnLogin.setOnClickListener(v -> {
            String carnet = etCarnet.getText().toString().toUpperCase().trim();
            String password = etPassword.getText().toString().trim();

            if (validarCampos(carnet, password)) {
                iniciarSesion(carnet, password);
            }
        });

        olvidoContra.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, OlvidoContraActivity.class);
            startActivity(intent);
        });

        registro.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });

    }

    /**
     * Verificar si ya hay una sesión guardada
     */
    private void verificarSesion() {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // Si ya hay sesión, ir directamente a MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Validar que los campos no estén vacíos
     */
    private boolean validarCampos(String carnet, String password) {
        if (carnet.isEmpty()) {
            etCarnet.setError("Ingresa tu carnet");
            etCarnet.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Iniciar sesión verificando credenciales
     */
    private void iniciarSesion(String carnet, String password) {
        try {


            // Obtener por carnet solamente para inicio rapido
            Usuarios usuario = db.getUsuarioByCarnet(carnet);

            // Verificar contraseña usando BCrypt
            boolean verified = Utilidades.verifyPassword(password, usuario.getContraseña());
            Log.d("LOGIN", "Verificación: " + verified);

            if (verified) {
                // Login exitoso
                guardarSesion(usuario);
                Toast.makeText(LoginActivity.this, "Bienvenido " + usuario.getNombres(), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                etPassword.setText("");
                etPassword.requestFocus();
            }

            if (detectarInternet.hayConexionInternet()){
                /*

                // Buscar usuario por carnet

                // Buscar en Firebase
                firebaseHelper.buscarUsuarioPorCarnet(
                        carnet,

                        new FirebaseHelper.FirebaseUsuarioCallback() {

                            @Override
                            public void onSuccess(Usuarios usuario) {

                                if (usuario == null) {
                                    Toast.makeText(LoginActivity.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                                    Log.d("LOGIN", "Usuario no encontrado");
                                    return;
                                }

                                Log.d("LOGIN", "Usuario encontrado: " + usuario.getNombres());
                                Log.d("LOGIN", "Hash almacenado: " + usuario.getContraseña());

                                // Verificar contraseña usando BCrypt
                                boolean verified = Utilidades.verifyPassword(password, usuario.getContraseña());
                                Log.d("LOGIN", "Verificación: " + verified);

                                if (verified) {
                                    // Login exitoso
                                    guardarSesion(usuario);
                                    Toast.makeText(LoginActivity.this, "Bienvenido " + usuario.getNombres(), Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                                    etPassword.setText("");
                                    etPassword.requestFocus();
                                }
                            }

                            @Override
                            public void onNotFound() {

                                Log.w("LOGIN",
                                        "⚠️ Usuario no encontrado");

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Usuario o contraseña incorrecta",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onError(String error) {

                                Log.e("LOGIN",
                                        "❌ Error: " + error);

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Error: " + error,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );

                 */
            }else {
                Toast.makeText(this, "Necesita conexion para iniciar sesion" , Toast.LENGTH_LONG).show();
            }


        } catch (Exception e) {
            Log.e("LOGIN_ERROR", "Error: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Guardar datos de sesión
     */
    private void guardarSesion(Usuarios usuario) {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putInt("usuario_id", usuario.getId_usuario());
        editor.putString("usuario_nombre", usuario.getNombres());
        editor.putString("usuario_apellidos", usuario.getApellidos());
        editor.putInt("usuario_tipo", usuario.getId_tipo()); // 1=Estudiante, 2=Admin
        editor.putString("usuario_carnet", usuario.getCarnet());
        editor.putString("usuario_correo", usuario.getCorreo());
        editor.putString("usuario_contra", usuario.getContraseña());
        editor.apply();
    }
}