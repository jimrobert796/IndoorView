package com.example.indoorview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CodigoVerificacionActivity extends AppCompatActivity {


    /**
     * Encargada visualmente mostrar y tomar el codigo de cambio de contraseña
     */

    private Button btnConfirmarCodigo;
    private ImageView btnRegresar;
    private EditText etCode1, etCode2, etCode3, etCode4;
    private TextView tvReenviarCodigo, tvCorreo;
    private String correo;
    private String codigoEsperado;
    private CountDownTimer countDownTimer;
    private boolean puedereenviar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.codigo_verificacion);

        // Obtener datos de la intent anterior
        correo = getIntent().getStringExtra("correo");
        codigoEsperado = getIntent().getStringExtra("codigo");

        inicializarVistas();
        configurarFocusEnEditTexts();
        iniciarTemporizador();

        // Asigamos el correo al que se envia para que vea el usuario
        tvCorreo.setText(correo);

        btnRegresar.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            finish();
        });

        btnConfirmarCodigo.setOnClickListener(v -> verificarCodigo());

        tvReenviarCodigo.setOnClickListener(v -> {
            if (puedereenviar) {
                reenviarCodigo();
            } else {
                Toast.makeText(this, "Espere antes de reenviar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void inicializarVistas() {
        btnConfirmarCodigo = findViewById(R.id.btn_confirmar_codigo);
        btnRegresar = findViewById(R.id.btn_regresar);
        etCode1 = findViewById(R.id.et_code_1);
        etCode2 = findViewById(R.id.et_code_2);
        etCode3 = findViewById(R.id.et_code_3);
        etCode4 = findViewById(R.id.et_code_4);
        tvReenviarCodigo = findViewById(R.id.tv_reenviar_codigo);
        tvCorreo = findViewById(R.id.tv_correo);
    }

    private void configurarFocusEnEditTexts() {
        // Cuando se escribe en el primer campo, pasar al segundo automáticamente
        etCode1.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        etCode2.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode3.requestFocus();
                } else if (s.length() == 0) {
                    etCode1.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        etCode3.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode4.requestFocus();
                } else if (s.length() == 0) {
                    etCode2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        etCode4.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    etCode3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }


    /**
     * Verifica si el codigo es correcto al enviado
     */
    private void verificarCodigo() {
        String codigoIngresado = etCode1.getText().toString() +
                etCode2.getText().toString() +
                etCode3.getText().toString() +
                etCode4.getText().toString();

        // Validar que se ingresaron los 4 dígitos
        if (codigoIngresado.length() != 4) {
            Toast.makeText(this, "Ingrese los 4 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Comparar con el código esperado
        if (codigoIngresado.equals(codigoEsperado)) {
            Toast.makeText(this, "Código correcto!", Toast.LENGTH_SHORT).show();

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            // Ir a la activity de nueva contraseña
            Intent intent = new Intent(CodigoVerificacionActivity.this, NuevaContraActivity.class);
            intent.putExtra("correo", correo);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show();
            // Limpiar campos
            etCode1.setText("");
            etCode2.setText("");
            etCode3.setText("");
            etCode4.setText("");
            etCode1.requestFocus();
        }
    }


    /**
     * Temporizador para re envio
     */
    private void iniciarTemporizador() {
        // Temporizador de 60 segundos para poder reenviar
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int segundos = (int) (millisUntilFinished / 1000);
                tvReenviarCodigo.setText(String.format("00:%02d reenviar código de confirmación", segundos));
                puedereenviar = false;
            }

            @Override
            public void onFinish() {
                tvReenviarCodigo.setText("Reenviar código de confirmación");
                puedereenviar = true;
            }
        };
        countDownTimer.start();
    }


    /**
     * Genera y envia el codigo
     */
    private void reenviarCodigo() {
        // Generar nuevo código
        String nuevoCodigoVerificacion = generarCodigoAleatorio();
        codigoEsperado = nuevoCodigoVerificacion;

        // Enviar correo con el nuevo código
        EmailHelper.enviarCodigoVerificacion(correo, nuevoCodigoVerificacion);

        Toast.makeText(this, "Código reenviado a " + correo, Toast.LENGTH_LONG).show();

        // Limpiar campos
        etCode1.setText("");
        etCode2.setText("");
        etCode3.setText("");
        etCode4.setText("");
        etCode1.requestFocus();

        // Reiniciar temporizador
        iniciarTemporizador();
    }


    /**
     * Generador de 4 dijitos random
     */
    private String generarCodigoAleatorio() {
        return String.format("%04d", (int) (Math.random() * 10000));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}