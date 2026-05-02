package com.example.indoorview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;

import com.example.indoorview.models.Eventos;

import java.util.List;

public class EventosAdapter extends RecyclerView.Adapter<EventosAdapter.EventoViewHolder> {

    private List<Eventos> eventosList;
    private Context context;
    private OnEventoLongClickListener onEventoLongClickListener;
    private OnEventoClickListener onEventoClickListener;

    // Interfaz para manejar long click
    public interface OnEventoLongClickListener {
        void onEventoLongClick(Eventos evento, int position);
    }

    // Interfaz para manejar click normal
    public interface OnEventoClickListener {
        void onEventoClick(Eventos evento, int position);
    }

    public EventosAdapter(List<Eventos> eventosList, Context context) {
        this.eventosList = eventosList;
        this.context = context;
    }

    // Setter para el listener de long click
    public void setOnEventoLongClickListener(OnEventoLongClickListener listener) {
        this.onEventoLongClickListener = listener;
    }

    // Setter para el listener de click normal
    public void setOnEventoClickListener(OnEventoClickListener listener) {
        this.onEventoClickListener = listener;
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_evento, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Eventos evento = eventosList.get(position);

        // Establecer datos
        holder.tvTituloEvento.setText(evento.getNombre());
        holder.tvDescripcion.setText(evento.getDescripcion());
        holder.tvHoraInicio.setText(evento.getFecha_inicio()); // O la hora si la guardas por separado
        holder.tvHoraFin.setText("A " + evento.getFecha_fin());

        // Click listener
        holder.cardEvento.setOnClickListener(v -> {
            if (onEventoClickListener != null) {
                onEventoClickListener.onEventoClick(evento, position);
            }
        });

        // Long click listener
        holder.cardEvento.setOnLongClickListener(v -> {
            if (onEventoLongClickListener != null) {
                onEventoLongClickListener.onEventoLongClick(evento, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return eventosList.size();
    }

    // Método para actualizar la lista
    public void updateList(List<Eventos> newList) {
        this.eventosList = newList;
        notifyDataSetChanged();
    }

    // Método para eliminar un evento de la lista
    public void removeEvento(int position) {
        if (position >= 0 && position < eventosList.size()) {
            eventosList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, eventosList.size());
        }
    }

    // ViewHolder
    public static class EventoViewHolder extends RecyclerView.ViewHolder {
        CardView cardEvento;
        TextView tvTituloEvento;
        TextView tvDescripcion;
        TextView tvHoraInicio;
        TextView tvHoraFin;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEvento = itemView.findViewById(R.id.card_evento);
            tvTituloEvento = itemView.findViewById(R.id.tv_titulo_evento);
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion);
            tvHoraInicio = itemView.findViewById(R.id.tv_hora_inicio);
            tvHoraFin = itemView.findViewById(R.id.tv_hora_fin);
        }
    }
}