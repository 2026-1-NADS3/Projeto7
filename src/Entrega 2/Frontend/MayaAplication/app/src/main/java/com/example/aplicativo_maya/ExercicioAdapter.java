package com.example.aplicativo_maya;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ExercicioAdapter extends RecyclerView.Adapter<ExercicioAdapter.ViewHolder> {

    private List<ExercicioPrescrito> exerciciosList;
    private Context context;

    public ExercicioAdapter(List<ExercicioPrescrito> exerciciosList, Context context) {
        this.exerciciosList = exerciciosList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exercicio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExercicioPrescrito ex = exerciciosList.get(position);

        holder.txtNomeExercicio.setText(ex.exercicioTitulo);
        holder.txtSeriesRepeticoes.setText("Séries: " + ex.series + "x");

        boolean isConcluido = ex.concluido;
        holder.imgStatus.setImageResource(
                isConcluido ? R.drawable.exercise_circle_complete
                        : R.drawable.exercise_circle_incomplete);

        if (isValidUrl(ex.fotoUrl)) {
            holder.imgIconeExercicio.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(ex.fotoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.circulo_vermelho)
                    .error(R.drawable.circulo_vermelho)
                    .into(holder.imgIconeExercicio);
        } else {
            holder.imgIconeExercicio.setVisibility(View.VISIBLE);
            holder.imgIconeExercicio.setImageResource(R.drawable.circulo_vermelho);
        }

        holder.itemExercicioRoot.setOnClickListener(v -> {
            Intent intent = new Intent(context, ExercicioInActivity.class);
            intent.putExtra("ID_PRESCRICAO", ex.id);
            intent.putExtra("TITULO", ex.exercicioTitulo);
            intent.putExtra("SERIES", ex.series);
            intent.putExtra("REPETICOES", ex.repeticoes);
            intent.putExtra("TEMPO", ex.tempoSegundos);
            intent.putExtra("OBSERVACOES", ex.observacoes);
            intent.putExtra("VIDEO_URL", ex.videoUrl);
            intent.putExtra("FOTO_URL", ex.fotoUrl);
            intent.putExtra("ROTINA_ID", ex.rotinaId);

            if (context instanceof Activity) {
                context.startActivity(intent);
                ((Activity) context).overridePendingTransition(0, 0);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exerciciosList.size();
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.isEmpty() && !url.equals("null")
                && (url.startsWith("http://") || url.startsWith("https://"));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNomeExercicio, txtSeriesRepeticoes;
        ImageView imgIconeExercicio, imgStatus;
        ConstraintLayout itemExercicioRoot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemExercicioRoot    = itemView.findViewById(R.id.itemExercicioRoot);
            imgIconeExercicio    = itemView.findViewById(R.id.imgIconeExercicio);
            txtNomeExercicio     = itemView.findViewById(R.id.txtNomeExercicio);
            txtSeriesRepeticoes  = itemView.findViewById(R.id.txtSeriesRepeticoes);
            imgStatus            = itemView.findViewById(R.id.imgStatus);
        }
    }
}