package com.example.indoorview;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class RegistroActivity extends AppCompatActivity {

    /// Usaremos lo que seria FIRESTORE y Storage para esto no hay problema
    /// tenemos un mes completo para las pruebas como tal
    /// por lo que el subir la informacion no seria un problema


    ImageView btnRegresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.registro);

        btnRegresar = findViewById(R.id.btn_regresar);

        btnRegresar.setOnClickListener(v -> finish());
    }
}
