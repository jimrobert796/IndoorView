package com.example.indoorview;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
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


    // Variables para datos de sesión
    private boolean usuarioLog;
    private int usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private int usuarioTipo;
    private String usuarioCarnet;
    private String usuarioCorreo;
    private FirebaseHelper firebaseHelper;
    private DetectarInternet detectarInternet;
    private PermissionManager permissionManager;

    private SyncManager syncManager;
    private Dialog loadingDialog; // Para la pantalla de carga

    private LinearLayout llContenedorVacio;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_eventos, container, false);


        // Obtener los datos de usuario
        obtenerDatosSesion();


        // Inicializar vistas
        rvEventos = view.findViewById(R.id.rv_eventos);
        fab = view.findViewById(R.id.fab_agregar);
        llContenedorVacio = view.findViewById(R.id.ll_contenedor_vacio);

        // Controlar visibilidad del FAB según tipo de usuario
        if (usuarioTipo == 1) {  // Estudiante
            fab.setVisibility(View.GONE);  // Ocultar botón de agregar
        } else {  // Administrador
            fab.setVisibility(View.VISIBLE);  // Mostrar botón de agregar
        }

        // Inicializar BD
        bdEventos = new Database(getContext());
        firebaseHelper = new FirebaseHelper();
        detectarInternet = new DetectarInternet(getContext());
        syncManager = new SyncManager(getContext(), bdEventos, firebaseHelper);
        permissionManager = PermissionManager.getInstance();


        // Configurar RecyclerView
        rvEventos.setLayoutManager(new LinearLayoutManager(getContext()));
        eventosList = new ArrayList<>();
        adapter = new EventosAdapter(eventosList, getContext());
        rvEventos.setAdapter(adapter);

        // Cargar eventos locales primero
        cargarEventos();


        if (detectarInternet.hayConexionInternet()){
            loadingDialog = new Dialog(getContext());
            View loadingView = LayoutInflater.from(getContext()).inflate(R.layout.progress_loading, null);
            loadingDialog.setContentView(loadingView);  // ← ESTO FALTA
            loadingDialog.setCancelable(false);

            // Mostrar diálogo de carga
            loadingDialog.show();

            // Cambiar mensaje
            TextView tvMessage = loadingView.findViewById(R.id.tv_loading_message);
            tvMessage.setText("Cargando eventos...");

            new android.os.Handler().postDelayed(() -> {
                syncManager.syncAllEventosWithClean(new SyncManager.SyncCallback() {
                    @Override
                    public void onSyncComplete() {
                        // Ahora SÍ carga los datos después de sincronizar
                        requireActivity().runOnUiThread(() -> {
                            cargarEventos();
                            loadingDialog.dismiss();
                            Toast.makeText(getContext(), "Eventos sincronizados", Toast.LENGTH_SHORT).show();


                        });
                    }

                    @Override
                    public void onSyncError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }, 1500);
        }else {
            cargarEventos();

            Toast.makeText(getContext(), "Sin conexion a internet ", Toast.LENGTH_LONG).show();
        }


        // Configurar listeners - SOLO para administrador
        if (usuarioTipo == 2) {
            adapter.setOnEventoLongClickListener((evento, position, view1) -> {
                mostrarMenuOpciones(view1, evento, position);
            });
        } else {
            // Estudiante: deshabilitar long click (no mostrar menú)
            adapter.setOnEventoLongClickListener(null);
        }

        adapter.setOnEventoClickListener((evento, position) -> {
            // Crear Intent y pasar los datos del evento
            Intent intent = new Intent(getActivity(), MapaEventoActivity.class);

            // Pasar toda la información del evento
            intent.putExtra("id_evento", evento.getId_evento());
            intent.putExtra("nombre_evento", evento.getNombre());
            intent.putExtra("descripcion_evento", evento.getDescripcion());
            intent.putExtra("fecha_inicio", evento.getFecha_inicio());
            intent.putExtra("hora_inicio", evento.getHora_inicio());
            intent.putExtra("fecha_fin", evento.getFecha_fin());
            intent.putExtra("hora_fin", evento.getHora_fin());
            intent.putExtra("longitud", evento.getLongitud());
            intent.putExtra("latitud", evento.getLatitud());

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

    private void actualizarEstadoVacio() {
        if (eventosList.isEmpty()) {
            rvEventos.setVisibility(View.GONE);
            llContenedorVacio.setVisibility(View.VISIBLE);
        } else {
            rvEventos.setVisibility(View.VISIBLE);
            llContenedorVacio.setVisibility(View.GONE);
        }
    }




    /**
     * Cargar eventos de la base de datos
     */
    private void cargarEventos() {
        eventosList.clear();
        eventosList.addAll(bdEventos.getEventos());
        adapter.notifyDataSetChanged();
        actualizarEstadoVacio();  // ← Agrega esta línea
    }

    private void mostrarMenuOpciones(View view, Eventos evento, int position) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);  // ✅ Usa la vista del item
        popupMenu.inflate(R.menu.menu_usuario);  // Crea tu propio menu_eventos.xml
        popupMenu.setGravity(Gravity.END);

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_editar) {
                editarEvento(evento);
                return true;
            } else if (itemId == R.id.action_eliminar) {
                mostrarDialogoConfirmacion("Confirmar eliminación", "Está seguro de que desea eliminar: \n\n" + evento.getNombre(), "Sí, eliminar", ()->{
                    eliminarEvento(evento, position);
                });
                return true;
            }
            return false;
        });

        popupMenu.show();
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
        intent.putExtra("hora_inicio", evento.getHora_inicio());
        intent.putExtra("fecha_fin", evento.getFecha_fin());
        intent.putExtra("hora_fin", evento.getHora_fin());
        intent.putExtra("latitud", evento.getLatitud());
        intent.putExtra("longitud", evento.getLongitud());
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

        // Detectamos la conexion a internet
        if (detectarInternet.hayConexionInternet()){

            // Cambiar estado a 0 (eliminado)
            evento.setEstado(0);
            int filasActualizadas = bdEventos.updateEvento(evento);


            // Guardar en Firebase
            firebaseHelper.eliminarEventoPermanentePorNombre(evento.getNombre(), new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {
                    Log.d("EVENTO", "✅ " + mensaje);
                }

                @Override
                public void onError(String error) {
                    Log.e("EVENTO", "❌ " + error);
                }
            });

            if (filasActualizadas > 0) {
                adapter.removeEvento(position);
                cargarEventos(); //acualizar datos en local
                Toast.makeText(getContext(), "Evento eliminado correctamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Error al eliminar el evento", Toast.LENGTH_SHORT).show();
            }

        }else {
            // HACER LA ELIMINACION SIN CONEXION Y ESPERAR QUE SE SUBAN LOS CAMBIOS

            // Estado 4 significa eliminado localmente para esperar a mandarlo a firebase
            evento.setEstado(4);
            int filasActualizadas = bdEventos.updateEvento(evento);

            if (filasActualizadas > 0) {
                adapter.removeEvento(position);
                cargarEventos(); //acualizar datos en local
                Toast.makeText(getContext(), "Evento eliminado correctamente sin conexion", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Error al eliminar el evento", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void mostrarDialogoConfirmacion(
            String titulo,
            String mensaje,
            String textoPositivo,
            Runnable onConfirm
    ) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
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

    // Obtener datos de la sesion
    private void obtenerDatosSesion() {
        // CORRECCIÓN: Usar requireContext() en lugar de getActivity()
        SharedPreferences prefs = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        // O también: getActivity().getSharedPreferences("sesion", Context.MODE_PRIVATE);

        usuarioLog = prefs.getBoolean("isLoggedIn", false);
        usuarioId = prefs.getInt("usuario_id", -1);
        usuarioNombre = prefs.getString("usuario_nombre", "");
        usuarioApellidos = prefs.getString("usuario_apellidos", "");
        usuarioTipo = prefs.getInt("usuario_tipo", 1);
        usuarioCarnet = prefs.getString("usuario_carnet", "");
        usuarioCorreo = prefs.getString("usuario_correo", "");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar eventos cuando el fragment vuelve a estar visible
        cargarEventos();
    }
}