package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class CodigoVerificacionActivity extends AppCompatActivity {

    private Button btnConfirmarCodigo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.codigo_verificacion);

        btnConfirmarCodigo = findViewById(R.id.btn_confirmar_codigo);

        btnConfirmarCodigo.setOnClickListener(v->{
            finish();
        });





    }
}
