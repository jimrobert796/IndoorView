package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.indoorview.models.Usuarios;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity para agregar un nuevo usuario o editar uno existente
 */
public class AgregarUsuarioActivity extends AppCompatActivity {

    // ==================== VISTAS ====================
    private EditText etCarnet, etNombres, etApellidos, etCorreo, etPassword;
    private ImageView btnRegresar;
    private TextInputLayout textInputLayout;
    private Spinner spinnerTipo;
    private Button btnCancelar, btnGuardar;

    // ==================== BASE DE DATOS ====================
    private Database db;

    // ==================== VARIABLES ====================
    private boolean esEdicion = false;
    private int usuarioIdEdicion = -1;

    // Array para mapear posiciones a valores
    private String[] tiposValores;

    private boolean cambiarContraseña = true;
    private String nuevaContraseñaHash = null;


    // Variables para datos de sesión
    private int usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private int usuarioTipo;
    private String usuarioCarnet;
    private String usuarioCorreo;
    private String usuarioContraseñaHash;

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
        TextInputLayout textInputLayout = findViewById(R.id.til_password); // Dale un ID a tu TextInputLayout
        spinnerTipo = findViewById(R.id.spinner_tipo_usuario);
        btnCancelar = findViewById(R.id.btn_cancelar);
        btnGuardar = findViewById(R.id.btn_guardar_usuario);
        btnRegresar = findViewById(R.id.btn_regresar);
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

                etPassword.setFocusable(false);
                etPassword.setFocusableInTouchMode(false);


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
        String carnet = etCarnet.getText().toString().trim();
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();


        // Obtener el tipo seleccionado (1 o 2)
        int idTipo = getTipoSeleccionado();

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
            if (resultado > 0) {
                Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Error al actualizar usuario", Toast.LENGTH_SHORT).show();
            }
        } else {
            String passwordHash = Utilidades.hashPassword(password);

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
            if (resultado > 0) {
                Toast.makeText(this, "Usuario agregado correctamente", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Error al agregar usuario", Toast.LENGTH_SHORT).show();
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

        // Validar que la contraseña tenga al menos un número
        if (!password.matches(".*\\d.*")) {
            etPassword.setError("La contraseña debe contener al menos un número");
            etPassword.requestFocus();
            return false;
        }

        // Validar que la contraseña tenga al menos una letra mayúscula
        if (!password.matches(".*[A-Z].*")) {
            etPassword.setError("La contraseña debe contener al menos una mayúscula");
            etPassword.requestFocus();
            return false;
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