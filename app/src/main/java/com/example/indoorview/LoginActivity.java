package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private Button btnLogin;

    private TextView olvidoContra;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        btnLogin = findViewById(R.id.btn_login);
        olvidoContra = findViewById(R.id.tv_forgot_password);

        btnLogin.setOnClickListener(v->{
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        });

        olvidoContra.setOnClickListener(v->{
            Intent intent = new Intent(LoginActivity.this, OlvidoContraActivity.class);
            startActivity(intent);
        });




    }
}
