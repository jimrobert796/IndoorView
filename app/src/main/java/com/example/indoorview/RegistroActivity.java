package com.example.indoorview;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorview.models.Usuarios;
import com.google.android.material.textfield.TextInputLayout;

public class RegistroActivity extends AppCompatActivity {

    /// Usaremos lo que seria FIRESTORE y Storage para esto no hay problema
    /// tenemos un mes completo para las pruebas como tal
    /// por lo que el subir la informacion no seria un problema
    // ==================== VISTAS ====================
    private EditText etCarnet, etNombres, etApellidos, etCorreo, etPassword, etPasswordConfirmar;
    private ImageView btnRegresar;
    private TextInputLayout textInputLayout;
    private Spinner spinnerTipo;
    private Button btnCancelar, btnRegistrar;
    private TextInputLayout tilPassword, tilPasswordConfirmar;
    private TextView tvPasswordConfirmar,tvPassword;


    private FirebaseHelper firebaseHelper;
    private DetectarInternet detectarInternet;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        detectarInternet = new  DetectarInternet(this);
        firebaseHelper = new FirebaseHelper();

        setContentView(R.layout.registro);


        inicializarVistas();
        configurarBotones();

    }

    private void inicializarVistas() {
        etCarnet = findViewById(R.id.et_carnet);
        etNombres = findViewById(R.id.et_nombres);
        etApellidos = findViewById(R.id.et_apellidos);
        etCorreo = findViewById(R.id.et_correo);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirmar = findViewById(R.id.et_passwordConfirmar);
        TextInputLayout textInputLayout = findViewById(R.id.til_password); // Dale un ID a tu TextInputLayout
        spinnerTipo = findViewById(R.id.spinner_tipo_usuario);
        btnCancelar = findViewById(R.id.btn_cancelar);
        btnRegistrar = findViewById(R.id.btn_registrar);
        btnRegresar = findViewById(R.id.btn_regresar);

        tilPasswordConfirmar = findViewById(R.id.til_passwordConfirmar);
        tvPasswordConfirmar = findViewById(R.id.tv_confirmar);
    }

    private boolean validarCampos() {
        // Obtener valores
        String carnet = etCarnet.getText().toString().toUpperCase().trim();
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmarPassword = etPasswordConfirmar.getText().toString().trim();

        // 1. Validar Carnet (mínimo 8 dígitos, solo números)
        if (carnet.isEmpty()) {
            etCarnet.setError("El carnet es requerido");
            etCarnet.requestFocus();
            return false;
        }
        if (!carnet.matches("[A-Z]{2,}\\d{4,}")) {
            etCarnet.setError("Formato inválido");
            etCarnet.requestFocus();
            return false;
        }

        // 2. Validar Nombres (solo letras y espacios, mínimo 3 caracteres)
        if (nombres.isEmpty()) {
            etNombres.setError("Los nombres son requeridos");
            etNombres.requestFocus();
            return false;
        }
        if (nombres.length() < 3) {
            etNombres.setError("Ingrese al menos 3 caracteres");
            etNombres.requestFocus();
            return false;
        }
        if (!nombres.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
            etNombres.setError("Solo se permiten letras");
            etNombres.requestFocus();
            return false;
        }

        // 3. Validar Apellidos (solo letras y espacios, mínimo 3 caracteres)
        if (apellidos.isEmpty()) {
            etApellidos.setError("Los apellidos son requeridos");
            etApellidos.requestFocus();
            return false;
        }
        if (apellidos.length() < 3) {
            etApellidos.setError("Ingrese al menos 3 caracteres");
            etApellidos.requestFocus();
            return false;
        }
        if (!apellidos.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
            etApellidos.setError("Solo se permiten letras");
            etApellidos.requestFocus();
            return false;
        }

        // 4. Validar Correo Electrónico
        if (correo.isEmpty()) {
            etCorreo.setError("El correo es requerido");
            etCorreo.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Ingrese un correo electrónico válido");
            etCorreo.requestFocus();
            return false;
        }
        // Validar dominio institucional (opcional)
        if (!correo.endsWith("@insti.edu.sv")) {
            // Es solo advertencia, no error
            Toast.makeText(this, "Sugerencia: Use correo institucional @insti.edu.sv", Toast.LENGTH_LONG).show();
        }

        // 5. Validar Contraseña
        if (password.isEmpty()) {
            etPassword.setError("La contraseña es requerida");
            etPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }


    /**
     * Configurar listeners de botones
     */
    private void configurarBotones() {
        // Botón Guardar con confirmación
        btnRegistrar.setOnClickListener(v -> {
            mostrarDialogoConfirmacion(
                    "Guardar Datos",
                    "¿Estas seguro de tus datos de registro?",
                    "Registrar",
                    () -> guardarUsuario()
            );
        });

        btnRegresar.setOnClickListener(v -> finish());

    }

    private void guardarUsuario() {
        // Validar que todos los campos estén llenos
        if (!validarCampos()) {
            return;
        }

        // Obtener datos de los campos
        String carnet = etCarnet.getText().toString().toUpperCase().trim();
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        password = Utilidades.hashPassword(password);


        if (detectarInternet.hayConexionInternet()) {

                Usuarios usuario = new Usuarios(
                        0,
                        1,
                        nombres,
                        apellidos,
                        correo,
                        carnet,
                        password,
                        1
                );

                // Guardar en Firebase
                firebaseHelper.guardarUsuarioEnFirestore(usuario, new FirebaseHelper.FirebaseCallback() {
                    @Override
                    public void onSuccess(String mensaje) {
                        Log.d("REGISTRO", "✅ " + mensaje);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("REGISTRO", "❌ " + error);
                    }
                });
            Toast.makeText(this, "Usuario regsitrado correctamente", Toast.LENGTH_SHORT).show();
            finish();
        }else {
            Toast.makeText(this, "Sin conexion para registro", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    public void mostrarDialogoConfirmacion(
            String titulo,
            String mensaje,
            String textoPositivo,
            Runnable onConfirm
    ) {
        AlertDialog dialog = new AlertDialog.Builder(this)
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
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(Color.parseColor("#2196F3"));

        // Opcional: También cambiar el botón negativo si quieres
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setTextColor(Color.parseColor("#2196F3"));
    }

}
