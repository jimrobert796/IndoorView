package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class NuevaContraActivity extends AppCompatActivity {


    private Button btnConfirmar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nueva_contrasena);

        btnConfirmar = findViewById(R.id.btn_confirmar);

        btnConfirmar.setOnClickListener(v->{
            Intent intent = new Intent(NuevaContraActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();

        });
    }
}
