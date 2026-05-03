package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PerfilFragment extends Fragment {

    private Button btn_usuarios;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_perfil, container, false);


        btn_usuarios = view.findViewById(R.id.btn_usuarios);

        btn_usuarios.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ListarUsuariosActivity.class);
            startActivity(intent);
        });


        return view;
    }
}