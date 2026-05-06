package com.example.indoorview;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorview.models.Usuarios;

import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;


public class UsuariosAdapter  extends RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder> {

    private List<Usuarios> listaUsuarios;
    private Context context;
    private UsuarioClickListener listener;

    /**
     * Interfaz para manejar clics en opciones del menú
     */
    public interface UsuarioClickListener {
        void onEditarClick(Usuarios usuario);
        void onEliminarClick(Usuarios usuario);
    }

    /**
     * Constructor del adaptador
     */
    public UsuariosAdapter(List<Usuarios> listaUsuarios, Context context, UsuarioClickListener listener) {
        this.listaUsuarios = listaUsuarios;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Inflamos el layout del item
     */
    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    /**
     * Vinculamos los datos con las vistas
     */
    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuarios usuario = listaUsuarios.get(position);

        // Mostrar nombre completo
        holder.tvNombre.setText(usuario.getNombres());

        // Mostrar carnet
        holder.tvCarnet.setText(usuario.getCarnet());

        // Click en menú de opciones
        holder.ivMenu.setOnClickListener(v -> mostrarMenuOpciones(v, usuario));
    }

    /**
     * Cantidad de items en la lista
     */
    @Override
    public int getItemCount() {
        return listaUsuarios.size();
    }

    /**
     * Mostrar PopupMenu con opciones Editar y Eliminar
     */
    private void mostrarMenuOpciones(View view, Usuarios usuario) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.menu_usuario);

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_editar) {
                listener.onEditarClick(usuario);
                return true;
            } else if (itemId == R.id.action_eliminar) {
                listener.onEliminarClick(usuario);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    /**
     * Actualizar la lista con nuevos datos
     */
    public void actualizarLista(List<Usuarios> nuevaLista) {
        this.listaUsuarios = nuevaLista;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder para cada item de usuario
     */
    public static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCarnet;
        ImageView ivMenu;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_nombre_usuario);
            tvCarnet = itemView.findViewById(R.id.tv_carnet_usuario);
            ivMenu = itemView.findViewById(R.id.iv_menu_usuario);
        }
    }
}