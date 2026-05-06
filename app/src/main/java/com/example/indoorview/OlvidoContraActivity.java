package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OlvidoContraActivity extends AppCompatActivity {

    private Button btnConfirmar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olvido_contra);

        btnConfirmar = findViewById(R.id.btn_confirmar);

        btnConfirmar.setOnClickListener(v->{
            Intent intent = new Intent(OlvidoContraActivity.this, CodigoVerificacionActivity.class);
            startActivity(intent);
        });





    }
}
