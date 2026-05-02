package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorview.models.Eventos;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class EventosFragment extends Fragment {

    private RecyclerView rvEventos;
    private EventosAdapter adapter;
    private FloatingActionButton fab;
    private Database bdEventos;
    private List<Eventos> eventosList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_eventos, container, false);

        // Inicializar vistas
        rvEventos = view.findViewById(R.id.rv_eventos);
        fab = view.findViewById(R.id.fab_agregar);

        // Inicializar BD
        bdEventos = new Database(getContext());

        // Configurar RecyclerView
        rvEventos.setLayoutManager(new LinearLayoutManager(getContext()));
        eventosList = new ArrayList<>();
        adapter = new EventosAdapter(eventosList, getContext());
        rvEventos.setAdapter(adapter);

        // Cargar eventos
        cargarEventos();

        // Configurar listeners del adaptador
        adapter.setOnEventoLongClickListener((evento, position) -> {
            mostrarDialogoOpciones(evento, position);
        });

        adapter.setOnEventoClickListener((evento, position) -> {
            // Aquí puedes mostrar detalles del evento si lo deseas
            Intent intent = new Intent(getActivity(), MapaEventoActivity.class);
            startActivity(intent);
            Toast.makeText(getContext(), "Evento: " + evento.getNombre(), Toast.LENGTH_SHORT).show();
        });

        // FAB para agregar evento
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AgregarEventoActivity.class);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Cargar eventos de la base de datos
     */
    private void cargarEventos() {
        eventosList.clear();
        eventosList.addAll(bdEventos.getEventos());
        adapter.notifyDataSetChanged();
    }

    /**
     * Mostrar diálogo con opciones de editar y eliminar
     */
    private void mostrarDialogoOpciones(Eventos evento, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Opciones del evento")
                .setMessage(evento.getNombre())
                .setPositiveButton("Editar", (dialog, which) -> {
                    editarEvento(evento);
                })
                .setNegativeButton("Eliminar", (dialog, which) -> {
                    confirmarEliminar(evento, position);
                })
                .setNeutralButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Editar evento: abrir AgregarEventoActivity con los datos del evento
     */
    private void editarEvento(Eventos evento) {
        Intent intent = new Intent(getActivity(), AgregarEventoActivity.class);
        intent.putExtra("es_edicion", true);
        intent.putExtra("id_evento", evento.getId_evento());
        intent.putExtra("nombre", evento.getNombre());
        intent.putExtra("descripcion", evento.getDescripcion());
        intent.putExtra("fecha_inicio", evento.getFecha_inicio());
        intent.putExtra("fecha_fin", evento.getFecha_fin());
        intent.putExtra("latitud", evento.getLatitud());
        intent.putExtra("longitud", evento.getLongitud());
        intent.putExtra("id_lugar", evento.getId_lugar());
        intent.putExtra("id_espacio", evento.getId_espacio());
        startActivity(intent);
    }

    /**
     * Confirmar eliminación del evento
     */
    private void confirmarEliminar(Eventos evento, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro de que desea eliminar este evento?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    eliminarEvento(evento, position);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Eliminar evento de la base de datos
     */
    private void eliminarEvento(Eventos evento, int position) {
        // Cambiar estado a 0 (eliminado)
        evento.setEstado(0);
        int filasActualizadas = bdEventos.updateEvento(evento);

        if (filasActualizadas > 0) {
            adapter.removeEvento(position);
            Toast.makeText(getContext(), "Evento eliminado correctamente", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error al eliminar el evento", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar eventos cuando el fragment vuelve a estar visible
        cargarEventos();
    }
}