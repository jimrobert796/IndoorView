package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorview.models.Usuarios;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity para agregar un nuevo usuario o editar uno existente
 */
public class AgregarUsuarioActivity extends AppCompatActivity {

    // ==================== VISTAS ====================
    private EditText etCarnet, etNombres, etApellidos, etCorreo, etPassword, etPasswordConfirmar;
    private ImageView btnRegresar;
    private TextInputLayout textInputLayout;
    private Spinner spinnerTipo;
    private Button btnCancelar, btnGuardar;
    private TextInputLayout tilPassword, tilPasswordConfirmar;
    private TextView tvPasswordConfirmar,tvPassword;

    // ==================== BASE DE DATOS ====================
    private Database db;

    // ==================== VARIABLES ====================
    private boolean esEdicion = false;
    private int usuarioIdEdicion = -1;

    // Array para mapear posiciones a valores
    private String[] tiposValores;

    private boolean cambiarContraseña = true;
    private String nuevaContraseñaHash = null;

    private String carnetOriginal;


    // Variables para datos de sesión
    private int usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private int usuarioTipo;
    private String usuarioCarnet;
    private String usuarioCorreo;
    private String usuarioContraseñaHash;

    // Utils
    private DetectarInternet detectarInternet;
    private FirebaseHelper firebaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_usuario);

        // Inicializar vistas
        inicializarVistas();

        // Configurar el Spinner
        configurarSpinner();

        // Inicializar base de datos
        db = new Database(this);

        firebaseHelper = new FirebaseHelper();
        detectarInternet = new DetectarInternet(this);

        // Verificar si estamos editando
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("es_edicion", false)) {
            esEdicion = true;
            usuarioIdEdicion = extras.getInt("usuario_id", -1);
            spinnerTipo.post(() -> cargarDatosUsuario());
        }

        // Configurar listeners de botones
        configurarBotones();
    }

    /**
     * Configurar el Spinner con los textos visibles
     */
    private void configurarSpinner() {
        // Cargar los valores numéricos desde resources
        tiposValores = getResources().getStringArray(R.array.tipos_usuario_valores);

        // Crear adapter solo con los textos visibles
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.tipos_usuario,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapter);
    }

    /**
     * Obtener el valor numérico del tipo seleccionado
     */
    private int getTipoSeleccionado() {
        int position = spinnerTipo.getSelectedItemPosition();
        try {
            return Integer.parseInt(tiposValores[position]);
        } catch (NumberFormatException e) {
            return 1; // Valor por defecto (Estudiante)
        }
    }

    /**
     * Seleccionar la posición del Spinner basada en el valor numérico
     */
    private void setTipoSeleccionado(int idTipo) {
        String valorStr = String.valueOf(idTipo);
        for (int i = 0; i < tiposValores.length; i++) {
            if (tiposValores[i].equals(valorStr)) {
                spinnerTipo.setSelection(i);
                return;
            }
        }
        spinnerTipo.setSelection(0); // Por defecto: Estudiante
    }

    /**
     * Inicializar referencias a las vistas
     */
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
        btnGuardar = findViewById(R.id.btn_guardar_usuario);
        btnRegresar = findViewById(R.id.btn_regresar);

        tilPasswordConfirmar = findViewById(R.id.til_passwordConfirmar);
        tvPasswordConfirmar = findViewById(R.id.tv_confirmar);
    }

    /**
     * Cargar datos del usuario para edición
     */
    private void cargarDatosUsuario() {


        if (usuarioIdEdicion != -1) {
            Usuarios usuario = db.getUsuarioById(usuarioIdEdicion);
            if (usuario != null) {
                // Llenar los campos con los datos del usuario
                etCarnet.setText(usuario.getCarnet());
                etNombres.setText(usuario.getNombres());
                etApellidos.setText(usuario.getApellidos());
                etCorreo.setText(usuario.getCorreo());
                etPassword.setText("12345678");

                nuevaContraseñaHash = usuario.getContraseña();
                carnetOriginal = usuario.getCarnet();

                etPassword.setFocusable(false);
                etPassword.setFocusableInTouchMode(false);
                tvPasswordConfirmar.setVisibility(View.GONE);
                tilPasswordConfirmar.setVisibility(View.GONE);


                // Deshabilitar el ícono de mostrar/ocultar (opcional)
                TextInputLayout textInputLayout = findViewById(R.id.til_password);
                textInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);


                // Seleccionar el tipo en el Spinner
                setTipoSeleccionado(usuario.getId_tipo());
            }
        }
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
                    "Guardar Usuario",
                    esEdicion ? "¿Deseas actualizar este usuario?" : "¿Deseas agregar este nuevo usuario?",
                    "Guardar",
                    () -> guardarUsuario()
            );
        });


        etPassword.setOnClickListener(v->{
            if (esEdicion && cambiarContraseña){
                mostrarDialogoConfirmacion(
                        "Nueva contraseña",
                        esEdicion ? "¿Deseas actualizar este usuario?" : "¿Deseas agregar este nuevo usuario?",
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
     * Guardar o actualizar usuario
     */
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


        // Obtener el tipo seleccionado (1 o 2)
        int idTipo = getTipoSeleccionado();

        // SI ES EN MODO DE EDITAR O MODIFICAR
        if (esEdicion) {
            String passwordFinal;
            if (cambiarContraseña == false) {
                passwordFinal = Utilidades.hashPassword(password);  // Usar nueva contraseña hasheada
                Log.d("CONTRASEÑA_HASH", "NUEVA CONTRASEÑA HASHED:"+ passwordFinal);
            } else {
                passwordFinal = nuevaContraseñaHash;  // Mantener la original
                Log.d("CONTRASEÑA_HASH", "SE MANTIENE LA CONTRA HASHED:"+ passwordFinal);
            }

            // ACTUALIZAR

            if (detectarInternet.hayConexionInternet()){

                Usuarios usuario = new Usuarios(
                        usuarioIdEdicion,
                        idTipo,
                        nombres,
                        apellidos,
                        correo,
                        carnet,
                        passwordFinal,
                        1
                );

                int resultado = db.actualizarUsuario(usuario);

                // Guardar en Firebase
                firebaseHelper.actualizarUsuarioPorCarnet(usuario, new FirebaseHelper.FirebaseCallback() {
                    @Override
                    public void onSuccess(String mensaje) {
                        Log.d("EVENTO", "✅ " + mensaje);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("EVENTO", "❌ " + error);
                    }
                });

                if (resultado > 0) {
                    Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Error al actualizar usuario", Toast.LENGTH_SHORT).show();
                }

            }else {

                // Guardar sin conexion a internet ESTADO 2 Modificado sin conexion
                Usuarios usuario = new Usuarios(
                        usuarioIdEdicion,
                        idTipo,
                        nombres,
                        apellidos,
                        correo,
                        carnet,
                        passwordFinal,
                        2
                );

                int resultado = db.actualizarUsuario(usuario);

                guardarEnSharedPreferencesModificar(resultado,usuarioCarnet, carnetOriginal);

                if (resultado > 0) {
                    Toast.makeText(this, "Usuario actualizado correctamente sin conexion", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Error al actualizar usuario", Toast.LENGTH_SHORT).show();
                }

            }
            // AGREGAR
        } else {
            String passwordHash = Utilidades.hashPassword(password);

            if (detectarInternet.hayConexionInternet()){
                Log.d("CONTRASEÑA_HASH", "NUEVO USUARIO POR ADMIN:"+ passwordHash);
                // INSERTAR NUEVO
                Usuarios usuario = new Usuarios(
                        0,
                        idTipo,
                        nombres,
                        apellidos,
                        correo,
                        carnet,
                        passwordHash,
                        1
                );

                long resultado = db.insertarUsuario(usuario);
                // Guardar en Firebase
                firebaseHelper.guardarUsuarioEnFirestore(usuario, new FirebaseHelper.FirebaseCallback() {
                    @Override
                    public void onSuccess(String mensaje) {
                        Log.d("EVENTO", "✅ " + mensaje);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("EVENTO", "❌ " + error);
                    }
                });

                if (resultado > 0) {
                    Toast.makeText(this, "Usuario agregado correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Error al agregar usuario", Toast.LENGTH_SHORT).show();
                }
            }else {
                // Sin conexion a internet guardar con estado 2
                Log.d("CONTRASEÑA_HASH", "NUEVO USUARIO POR ADMIN:"+ passwordHash);
                // INSERTAR NUEVO
                Usuarios usuario = new Usuarios(
                        0,
                        idTipo,
                        nombres,
                        apellidos,
                        correo,
                        carnet,
                        passwordHash,
                        3 // SIN CONEXION PARA SINCRONIZAR
                );

                long resultado = db.insertarUsuario(usuario);
                if (resultado > 0) {
                    Toast.makeText(this, "Usuario agregado correctamente sin conexion", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Error al agregar usuario", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
        if (!correo.endsWith("@ugb.edu.sv")) {
            // Es solo advertencia, no error
            Toast.makeText(this, "Sugerencia: Use correo institucional @ugb.edu.sv", Toast.LENGTH_LONG).show();
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

        if (!cambiarContraseña) {
            // Validar que no esté vacía
            if (password.isEmpty()) {
                etPassword.setError("Ingrese la nueva contraseña");
                etPassword.requestFocus();
                return false;
            }

            // VALIDAR QUE LAS CONTRASEÑAS COINCIDAN
            if (confirmarPassword.isEmpty()) {
                etPasswordConfirmar.setError("Confirme su nueva contraseña");
                etPasswordConfirmar.requestFocus();
                return false;
            }

            if (!password.equals(confirmarPassword)) {
                etPasswordConfirmar.setError("Las contraseñas no coinciden");
                etPasswordConfirmar.requestFocus();
                return false;
            }
        }



        return true;
    }

    /**
     * Liberar recursos al destruir
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
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

    // asi sabremos cual vamos a modificar gracias a su id 1 2 3 4 5 6 etc
    private void guardarEnSharedPreferencesModificar(int idUsuarioEditando, String usuarioCarnet, String usuarioCarnetOriginal){
        SharedPreferences prefs = getSharedPreferences("usuarios_pendientes_modificar", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("pendiente_" + idUsuarioEditando, true);
        editor.putString("nombre_original_" + idUsuarioEditando, usuarioCarnetOriginal);
        editor.putString("nombre_actual_" + idUsuarioEditando, usuarioCarnet);
        editor.apply();
    }


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
        usuarioCorreo = prefs.getString("usuario_contra", "");
    }


}