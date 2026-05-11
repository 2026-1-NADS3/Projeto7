package com.example.aplicativo_maya;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.aplicativo_maya.api.AgendamentoResponse;
import com.example.aplicativo_maya.api.MayaRpgApi;
import com.example.aplicativo_maya.api.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgendamentosFragment extends Fragment {

    private LinearLayout layoutAgendados, layoutConcluidos;
    private TextView btnAgendado, btnConcluido;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MayaRpgApi api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agendamentos, container, false);

        layoutAgendados = view.findViewById(R.id.layoutAgendados);
        layoutConcluidos = view.findViewById(R.id.layoutConcluidos);
        btnAgendado = view.findViewById(R.id.btnToggleAgendado);
        btnConcluido = view.findViewById(R.id.btnToggleConcluido);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        btnAgendado.setOnClickListener(v -> showAgendados());
        btnConcluido.setOnClickListener(v -> showConcluidos());
        swipeRefreshLayout.setOnRefreshListener(this::carregarAgendamentos);

        api = RetrofitClient.getInstance().create(MayaRpgApi.class);

        swipeRefreshLayout.setRefreshing(true);
        carregarAgendamentos();

        return view;
    }

    private void carregarAgendamentos() {
        SharedPreferences sharedPref = getActivity().getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
        String token = sharedPref.getString("JWT_TOKEN", null);

        if (token == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        api.getAgendamentos("Bearer " + token).enqueue(new Callback<List<AgendamentoResponse>>() {
            @Override
            public void onResponse(Call<List<AgendamentoResponse>> call, Response<List<AgendamentoResponse>> response) {

                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    popularListas(response.body());
                } else {
                    Log.e("API_ERROR", "Erro: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<AgendamentoResponse>> call, Throwable t) {
                // Para a animação de refresh
                swipeRefreshLayout.setRefreshing(false);
                Log.e("API_FAILURE", t.getMessage());
            }
        });
    }

    private void popularListas(List<AgendamentoResponse> agendamentos) {
        layoutAgendados.removeAllViews();
        layoutConcluidos.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (AgendamentoResponse ag : agendamentos) {
            View itemView;
            if ("CONCLUIDO".equalsIgnoreCase(ag.getStatus())) {
                itemView = inflater.inflate(R.layout.item_agendamentos_concluido, layoutConcluidos, false);
                preencherCampos(itemView, ag, true);
                layoutConcluidos.addView(itemView);
            } else {
                itemView = inflater.inflate(R.layout.item_agendamento, layoutAgendados, false);
                preencherCampos(itemView, ag, false);
                layoutAgendados.addView(itemView);
            }

            itemView.setClickable(true);
            itemView.setFocusable(true);
            itemView.setOnClickListener(v -> {
                Log.d("SOL_DEBUG", "Card clicado! Paciente: " + ag.getPacienteNome());

                try {
                    Intent intent = new Intent(v.getContext(), DetalhesAgendamentoActivity.class);
                    intent.putExtra("agendamento", ag);
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    Log.e("SOL_DEBUG", "Erro ao abrir Detalhes: " + e.getMessage());
                }
            });
        }
    }

    private void preencherCampos(View view, AgendamentoResponse ag, boolean isConcluido) {
        TextView txtNome = view.findViewById(isConcluido ? R.id.txtCodigoConcluido : R.id.txtCodigo);
        TextView txtObs = view.findViewById(isConcluido ? R.id.txtSubtituloConcluido : R.id.txtSubtitulo);
        TextView txtStatus = view.findViewById(isConcluido ? R.id.txtStatusTagConcluido : R.id.txtStatusTag);

        TextView txtMes = view.findViewById(isConcluido ? R.id.txtMesConcluido : R.id.txtMes);
        TextView txtDia = view.findViewById(isConcluido ? R.id.txtDiaConcluido : R.id.txtDia);
        TextView txtHora = view.findViewById(isConcluido ? R.id.txtDiaSemanaHoraConcluido : R.id.txtDiaSemanaHora);

        txtNome.setText(ag.getPacienteNome());
        txtObs.setText(ag.getObservacao() != null ? ag.getObservacao() : "Sem observações");
        txtStatus.setText(ag.getStatus().toUpperCase());

        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdfInput.parse(ag.getDataHora());

            if (date != null) {
                txtMes.setText(new SimpleDateFormat("MMMM", new Locale("pt", "BR")).format(date));
                txtDia.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(date));
                String diaSemana = new SimpleDateFormat("EEE", new Locale("pt", "BR")).format(date).toUpperCase();
                String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
                txtHora.setText(diaSemana + "\n" + hora);
            }
        } catch (Exception e) {
            Log.e("DATE_PARSE", "Erro ao formatar data: " + ag.getDataHora());
        }
    }

    private void showAgendados() {
        layoutAgendados.setVisibility(View.VISIBLE);
        layoutConcluidos.setVisibility(View.GONE);
        btnAgendado.setBackgroundResource(R.drawable.bg_toggle_active_teal);
        btnConcluido.setBackground(null);
    }

    private void showConcluidos() {
        layoutAgendados.setVisibility(View.GONE);
        layoutConcluidos.setVisibility(View.VISIBLE);
        btnConcluido.setBackgroundResource(R.drawable.bg_toggle_active_coral);
        btnAgendado.setBackground(null);
    }
}