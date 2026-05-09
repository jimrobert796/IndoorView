package com.example.indoorview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorview.models.Usuarios;
import com.google.android.material.textfield.TextInputLayout;

public class EditarPerfilActivity extends AppCompatActivity {




    ///  HAY QUE PULIR LA VERSION DE USUARIO NORMAL Y LA VERSION DE USUARIO ADMIN TENGO SUEÑO ASI QUE DEJARE ESTO POR AQUI

    // Vistas
    private ImageView btnRegresar;
    private EditText etCarnet, etNombres, etApellidos, etCorreo, etPassword, etPasswordConfirmar;
    private TextInputLayout tilPassword, tilPasswordConfirmar;
    private TextView tvPasswordConfirmar;
    private Button btnCancelar, btnGuardar;

    // Base de datos
    private Database db;

    // Variables para datos de sesión
    private int usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private int usuarioTipo;
    private String usuarioCarnet;
    private String usuarioCorreo;
    private String usuarioContraseñaHash;

    // Variables para control de cambio de contraseña
    private boolean cambiarContraseña = true;
    private String nuevaContraseñaHash = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        // Inicializar vistas
        inicializarVistas();

        // Inicializar base de datos
        db = new Database(this);

        // Obtener datos de sesión
        obtenerDatosSesion();

        // Cargar datos del usuario en los campos
        cargarDatosUsuario();

        // Configurar listeners
        configurarBotones();
    }

    /**
     * Inicializar referencias a las vistas
     */
    private void inicializarVistas() {
        btnRegresar = findViewById(R.id.btn_regresar);
        etCarnet = findViewById(R.id.et_carnet);
        etNombres = findViewById(R.id.et_nombres);
        etApellidos = findViewById(R.id.et_apellidos);
        etCorreo = findViewById(R.id.et_correo);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirmar = findViewById(R.id.et_passwordConfirmar);
        tilPassword = findViewById(R.id.til_password);
        tilPasswordConfirmar = findViewById(R.id.til_passwordConfirmar);
        tvPasswordConfirmar = findViewById(R.id.tv_confirmar);
        btnCancelar = findViewById(R.id.btn_cancelar);
        btnGuardar = findViewById(R.id.btn_guardar_usuario);
    }

    /**
     * Obtener los datos de sesión
     */
    private void obtenerDatosSesion() {
        // CORRECCIÓN: Usar requireContext() en lugar de getActivity()
        SharedPreferences prefs = this.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        // O también: getActivity().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        usuarioId = prefs.getInt("usuario_id", -1);
        usuarioNombre = prefs.getString("usuario_nombre", "");
        usuarioApellidos = prefs.getString("usuario_apellidos", "");
        usuarioTipo = prefs.getInt("usuario_tipo", 1);
        usuarioCarnet = prefs.getString("usuario_carnet", "");
        usuarioCorreo = prefs.getString("usuario_correo", "");
        usuarioContraseñaHash = prefs.getString("usuario_contra", "");
    }

    /**
     * Cargar datos del usuario desde la base de datos
     */
    private void cargarDatosUsuario() {

                // Llenar los campos con los datos del usuario
                etCarnet.setText(usuarioNombre);
                etNombres.setText(usuarioNombre);
                etApellidos.setText(usuarioApellidos);
                etCorreo.setText(usuarioCorreo);
                etPassword.setText("12345678");

                nuevaContraseñaHash = usuarioContraseñaHash;

                etPassword.setFocusable(false);
                etPassword.setFocusableInTouchMode(false);


                tvPasswordConfirmar.setVisibility(View.GONE);
                tilPasswordConfirmar.setVisibility(View.GONE);



                // Deshabilitar el ícono de mostrar/ocultar (opcional)
                TextInputLayout textInputLayout = findViewById(R.id.til_password);
                textInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);

    }


    /**
     * Configurar listeners de botones
     */
    private void configurarBotones() {
        // Botón Cancelar con confirmación
        btnCancelar.setOnClickListener(v -> {
            mostrarDialogoConfirmacion(
                    "Cancelar",
                    "¿Estás seguro que deseas cancelar? Los cambios no guardados se perderán.",
                    "Sí, cancelar",
                    () -> finish()
            );
        });

        // Botón Guardar con confirmación
        btnGuardar.setOnClickListener(v -> {
            mostrarDialogoConfirmacion(
                    "Guardar Cambios",
                    "¿Deseas actualizar tus datos?",
                    "Guardar",
                    () -> guardarUsuario()
            );
        });


        etPassword.setOnClickListener(v->{
            if (cambiarContraseña){
                mostrarDialogoConfirmacion(
                        "Nueva contraseña",
                        "¿Deseas actualizar tu contraseña ?",
                        "Confirmar",
                        () ->{
                            cambiarContraseña = false;
                            //mostrarDialogoCambioContraseña();

                            tvPasswordConfirmar.setVisibility(View.VISIBLE);
                            tilPasswordConfirmar.setVisibility(View.VISIBLE);

                            etPasswordConfirmar.setEnabled(true);


                            // Habilitar el campo de contraseña
                            etPassword.setEnabled(true);
                            etPassword.setFocusable(true);
                            etPassword.setFocusableInTouchMode(true);
                            etPassword.requestFocus();
                            etPassword.setText("");
                            TextInputLayout tilPassword = findViewById(R.id.til_password);
                            tilPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

                        }
                );
            }

        });


        btnRegresar.setOnClickListener(v -> finish());



    }

    /**
     * Guardar cambios del usuario
     */
    private void guardarUsuario() {
        // Validar campos
        if (!validarCampos()) {
            return;
        }

        // Obtener datos de los campos
        String carnet = etCarnet.getText().toString().trim();
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();

        // Determinar la contraseña final
        String passwordFinal;
        if (cambiarContraseña && nuevaContraseñaHash != null) {
            passwordFinal = nuevaContraseñaHash;
        } else {
            passwordFinal = usuarioContraseñaHash; // Mantener la original
        }

        // Crear objeto usuario (tipo 1 por defecto, se mantiene el original)
        Usuarios usuario = new Usuarios(
                usuarioId,
                usuarioTipo, // Mantener el tipo original (no editable)
                nombres,
                apellidos,
                correo,
                carnet,
                passwordFinal,
                1
        );

        // Actualizar en la base de datos
        int resultado = db.actualizarUsuario(usuario);

        if (resultado > 0) {
            // Actualizar SharedPreferences
            actualizarSesion(nombres, apellidos, correo);

            Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Actualizar datos de sesión en SharedPreferences
     */
    private void actualizarSesion(String nombres, String apellidos, String correo) {
        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("usuario_nombre", nombres);
        editor.putString("usuario_apellidos", apellidos);
        editor.putString("usuario_correo", correo);
        editor.apply();
    }

    /**
     * Validar que todos los campos estén llenos
     */
    private boolean validarCampos() {
        // Obtener valores
        String carnet = etCarnet.getText().toString().toUpperCase().trim();
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();



        return true;
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
}