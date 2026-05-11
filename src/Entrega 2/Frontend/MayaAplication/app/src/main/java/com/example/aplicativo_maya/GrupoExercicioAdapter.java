package com.example.aplicativo_maya;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GrupoExercicioAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Rotina> rotinasList;
    private Context context;
    private final String[] coresBackground = {"#32C0D2", "#32A2D2", "#32D2BF", "#32C2D2"};

    private static final int TYPE_ROTINA = 0;
    private static final int TYPE_SEPARADOR = 1;

    public GrupoExercicioAdapter(List<Rotina> rotinasList, Context context) {
        this.rotinasList = rotinasList;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        if (rotinasList.get(position).id == -1) return TYPE_SEPARADOR;
        return TYPE_ROTINA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SEPARADOR) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_separator, parent, false);
            return new SeparadorViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_grupo_exercicios, parent, false);
            return new RotinaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Rotina rotina = rotinasList.get(position);

        if (holder instanceof SeparadorViewHolder) {
            ((SeparadorViewHolder) holder).txtSeparador.setText("CONCLUÍDAS");
        } else if (holder instanceof RotinaViewHolder) {
            RotinaViewHolder rHolder = (RotinaViewHolder) holder;
            rHolder.txtDia.setText(rotina.nome);
            rHolder.txtQuantidade.setText(rotina.totalExercicios + " Exercícios");

            int tempoEstimado = rotina.totalExercicios * 3;
            rHolder.txtTempo.setText("Tempo: " + tempoEstimado + " min");

            String corHex = coresBackground[position % coresBackground.length];
            rHolder.imgBackground.setColorFilter(Color.parseColor(corHex), PorterDuff.Mode.SRC_IN);
            rHolder.btnOpen.setTextColor(Color.parseColor(corHex));

            rHolder.itemRoot.setOnClickListener(v -> {
                Intent intent = new Intent(context, ListaExercicioActivity.class);
                intent.putExtra("ROTINA_ID", (long) rotina.id);
                intent.putExtra("ROTINA_NOME", rotina.nome);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return rotinasList.size();
    }

    public static class RotinaViewHolder extends RecyclerView.ViewHolder {
        TextView txtDia, txtQuantidade, txtTempo;
        ImageView imgBackground, imgIlustracao;
        Button btnOpen;
        ConstraintLayout itemRoot;

        public RotinaViewHolder(@NonNull View itemView) {
            super(itemView);
            itemRoot = itemView.findViewById(R.id.itemRoot);
            imgBackground = itemView.findViewById(R.id.imgBackground);
            txtDia = itemView.findViewById(R.id.txtDia);
            txtQuantidade = itemView.findViewById(R.id.txtQuantidade);
            txtTempo = itemView.findViewById(R.id.txtTempo);
            imgIlustracao = itemView.findViewById(R.id.imgIlustracao);
            btnOpen = itemView.findViewById(R.id.btnOpen);
        }
    }

    public static class SeparadorViewHolder extends RecyclerView.ViewHolder {
        TextView txtSeparador;
        public SeparadorViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSeparador = itemView.findViewById(R.id.txtSeparador);
        }
    }
}