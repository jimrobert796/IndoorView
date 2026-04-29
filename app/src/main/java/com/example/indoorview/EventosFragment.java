package com.example.indoorview;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class EventosFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_eventos, container, false);

        // Referencia al FAB
        FloatingActionButton fab = view.findViewById(R.id.fab_agregar);

        fab.setOnClickListener(v -> {
            // Abrir la nueva actividad
            Intent intent = new Intent(getActivity(), AgregarEventoActivity.class);
            startActivity(intent);
        });

        return view;
    }
}