package com.example.indoorview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PerfilFragment extends Fragment {

    private Button btn_usuarios, btnCerrarSession;
    private TextView tvNombreUsuario,tvApellidosUsuario, tvCarnetUsuario, tvCorreoUsuario;

    // Variables para datos de sesión
    private boolean usuarioLog;
    private int usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private int usuarioTipo;
    private String usuarioCarnet;
    private String usuarioCorreo;

    private ImageView btnEditarPerfil;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // Inicializar vistas
        btn_usuarios = view.findViewById(R.id.btn_usuarios);
        btnCerrarSession = view.findViewById(R.id.btn_cerrar_sesion);
        tvNombreUsuario = view.findViewById(R.id.tv_nombres);
        tvApellidosUsuario = view.findViewById(R.id.tv_apellidos);
        tvCarnetUsuario = view.findViewById(R.id.tv_carnet);
        tvCorreoUsuario = view.findViewById(R.id.tv_correo);
        btnEditarPerfil = view.findViewById(R.id.btn_editar_perfil);

        // Obtener datos de sesión
        obtenerDatosSesion();

        // Mostrar datos del usuario
        mostrarDatosUsuario();

        // Configurar visibilidad del botón según el tipo de usuario
        configurarBotonesPorTipo();

        btn_usuarios.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ListarUsuariosActivity.class);
            startActivity(intent);
        });
        btnCerrarSession.setOnClickListener(v -> {
            mostrarDialogoConfirmacion("Cerrar Sesión", "¿Estás seguro que deseas cerrar sesión?","Sí, cerrar", ()->{
                // Limpiar SharedPreferences
                SharedPreferences prefs = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                // Mostrar mensaje
                Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

                // Redirigir al LoginActivity
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // Finalizar la actividad actual
                requireActivity().finish();
            });
        });
        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditarPerfilActivity.class);
            startActivity(intent);
        });


        return view;
    }

    /**
     * Obtener los datos de sesión
     */
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

    /**
     * Mostrar datos del usuario en las vistas
     */
    private void mostrarDatosUsuario() {
        Log.d("PERFIL", "Nombre: " + usuarioNombre);
        Log.d("PERFIL", "Apellidos: " + usuarioApellidos);
        Log.d("PERFIL", "Carnet: " + usuarioCarnet);
        Log.d("PERFIL", "Correo: " + usuarioCorreo); // 👈 Verifica qué muestra

        if (tvNombreUsuario != null) {
            tvNombreUsuario.setText(usuarioNombre);
        }
        if (tvApellidosUsuario != null) {
            tvApellidosUsuario.setText(usuarioApellidos);
        }
        if (tvCarnetUsuario != null) {
            tvCarnetUsuario.setText(usuarioCarnet);
        }
        if (tvCorreoUsuario != null) {
            tvCorreoUsuario.setText(usuarioCorreo);
        }
    }

    /**
     * Configurar botones según el tipo de usuario
     */
    private void configurarBotonesPorTipo() {
        // Solo el administrador (tipo 2) puede ver el botón de usuarios
        if (usuarioTipo == 2) {
            btn_usuarios.setVisibility(View.VISIBLE);
        } else {
            btn_usuarios.setVisibility(View.GONE);
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
}