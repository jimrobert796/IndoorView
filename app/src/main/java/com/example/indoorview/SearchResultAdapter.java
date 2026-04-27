package com.example.indoorview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.indoorview.SearchManager.SearchResult;
import java.util.List;

/**
 * Adaptador para mostrar resultados de búsqueda en RecyclerView
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<SearchResult> resultados;
    private OnResultClickListener listener;
    private Context context;

    // ════════════════════════════════════════════════════════════════
    // INTERFAZ DE CALLBACK
    // ════════════════════════════════════════════════════════════════
    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }

    // ════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════
    public SearchResultAdapter(List<SearchResult> resultados, Context context) {
        this.resultados = resultados;
        this.context = context;
    }

    public void setOnResultClickListener(OnResultClickListener listener) {
        this.listener = listener;
    }

    // ════════════════════════════════════════════════════════════════
    // MÉTODOS DEL ADAPTADOR
    // ════════════════════════════════════════════════════════════════

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = resultados.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return resultados.size();
    }

    /**
     * Actualizar los resultados
     */
    public void actualizarResultados(List<SearchResult> nuevosResultados) {
        this.resultados = nuevosResultados;
        notifyDataSetChanged();
    }

    // ════════════════════════════════════════════════════════════════
    // VIEW HOLDER
    // ════════════════════════════════════════════════════════════════

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView tvNombre;
        private TextView tvDescripcion;
        private ImageView ivColor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardResultado);
            tvNombre = itemView.findViewById(R.id.tv_nombre_resultado);
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion_resultado);
            ivColor = itemView.findViewById(R.id.iv_color_indicador);
        }

        public void bind(SearchResult result) {
            tvNombre.setText(result.nombre);

            // Descripción: mostrar solo primeras líneas
            String desc = result.descripcion;
            if (desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            tvDescripcion.setText(desc);


            // Color indicador
            try {
                int color = android.graphics.Color.parseColor(result.color);
                ivColor.setBackgroundColor(color);
            } catch (Exception e) {
                ivColor.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
            }

            // Click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResultClick(result);
                }
            });
        }
    }
}