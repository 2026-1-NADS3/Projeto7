package com.example.aplicativo_maya;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.aplicativo_maya.api.AgendamentoResponse;
import com.example.aplicativo_maya.api.MayaRpgApi;
import com.example.aplicativo_maya.api.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomepageFragment extends Fragment {

    private LinearLayout containerAgendamentos;
    private View cardEmptyState;
    private SwipeRefreshLayout swipeRefreshHome;
    private MayaRpgApi api;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_homepage, container, false);

        containerAgendamentos = view.findViewById(R.id.containerAgendamentos);
        cardEmptyState = view.findViewById(R.id.cardEmptyState);
        swipeRefreshHome = view.findViewById(R.id.swipeRefreshHome);

        Button btnConsultar = view.findViewById(R.id.btnConsultar);
        Button btnContatar = view.findViewById(R.id.btnContatar);
        Button btnCadastrarAgora = view.findViewById(R.id.btnCadastrarAgora);

        if (btnConsultar != null) {
            btnConsultar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showFragment(1);
                }
            });
        }

        if (btnContatar != null) {
            btnContatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showFragment(3);
                }
            });
        }

        if (btnCadastrarAgora != null) {
            btnCadastrarAgora.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showFragment(2);
                }
            });
        }

        api = RetrofitClient.getInstance().create(MayaRpgApi.class);

        if (swipeRefreshHome != null) {
            swipeRefreshHome.setOnRefreshListener(this::carregarAgendamentosAtalho);
        }

        carregarAgendamentosAtalho();

        return view;
    }

    private void carregarAgendamentosAtalho() {
        SharedPreferences sharedPref = getActivity().getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
        String token = sharedPref.getString("JWT_TOKEN", null);

        if (token == null) {
            if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(false);
            return;
        }

        if (swipeRefreshHome != null && !swipeRefreshHome.isRefreshing()) {
            swipeRefreshHome.setRefreshing(true);
        }

        api.getAgendamentos("Bearer " + token).enqueue(new Callback<List<AgendamentoResponse>>() {
            @Override
            public void onResponse(Call<List<AgendamentoResponse>> call, Response<List<AgendamentoResponse>> response) {
                if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(false);

                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    List<AgendamentoResponse> lista = response.body();

                    if (lista.isEmpty()) {
                        cardEmptyState.setVisibility(View.VISIBLE);
                        containerAgendamentos.setVisibility(View.GONE);
                    } else {
                        cardEmptyState.setVisibility(View.GONE);
                        containerAgendamentos.setVisibility(View.VISIBLE);
                        ordenarAgendamentos(lista);
                        popularAtalhos(lista);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AgendamentoResponse>> call, Throwable t) {
                if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(false);
                Log.e("HOME_API", "Erro: " + t.getMessage());
            }
        });
    }

    private void ordenarAgendamentos(List<AgendamentoResponse> lista) {
        Collections.sort(lista, (a1, a2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                return sdf.parse(a1.getDataHora()).compareTo(sdf.parse(a2.getDataHora()));
            } catch (Exception e) {
                return 0;
            }
        });
    }

    private void popularAtalhos(List<AgendamentoResponse> agendamentos) {
        containerAgendamentos.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        int indexProximo = -1;
        for (int i = 0; i < agendamentos.size(); i++) {
            if (!"CONCLUIDO".equalsIgnoreCase(agendamentos.get(i).getStatus())) {
                indexProximo = i;
                break;
            }
        }

        for (int i = 0; i < agendamentos.size(); i++) {
            AgendamentoResponse ag = agendamentos.get(i);
            View itemView = inflater.inflate(R.layout.item_agendamento_atalho, containerAgendamentos, false);

            TextView txtData = itemView.findViewById(R.id.txtDataAtalho);
            TextView txtTitulo = itemView.findViewById(R.id.txtTituloAtalho);
            TextView txtHora = itemView.findViewById(R.id.txtHoraAtalho);
            TextView txtDesc = itemView.findViewById(R.id.txtDescricaoAtalho);
            CardView card = itemView.findViewById(R.id.cardConteudoAtalho);
            View linhaSup = itemView.findViewById(R.id.linhaSuperior);
            View linhaInf = itemView.findViewById(R.id.linhaInferior);
            ImageView dot = itemView.findViewById(R.id.imgDot);

            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdfInput.parse(ag.getDataHora());
                txtData.setText(new SimpleDateFormat("dd/MM", Locale.getDefault()).format(date));
                txtHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
            } catch (Exception e) {
                txtData.setText("--/--");
            }

            txtTitulo.setText(ag.getStatus().equalsIgnoreCase("CONCLUIDO") ? "Finalização" : "Consulta");
            txtTitulo.setText(ag.getPacienteNome() != null ? ag.getPacienteNome() : "Consulta");
            txtDesc.setText(ag.getObservacao() != null ? ag.getObservacao() : "Sem descrição");

            if (i == 0) linhaSup.setVisibility(View.INVISIBLE);
            if (i == agendamentos.size() - 1) linhaInf.setVisibility(View.INVISIBLE);

            if (i == indexProximo) {
                card.setCardBackgroundColor(Color.parseColor("#2EC4B6"));
                txtTitulo.setTextColor(Color.WHITE);
                txtHora.setTextColor(Color.WHITE);
                txtDesc.setTextColor(Color.parseColor("#E0F2F1"));
                dot.setImageResource(R.drawable.bg_circle_dark);
                dot.setColorFilter(Color.parseColor("#2EC4B6"));
            } else if ("CONCLUIDO".equalsIgnoreCase(ag.getStatus())) {
                card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                dot.setColorFilter(Color.parseColor("#CCCCCC"));
                linhaSup.setBackgroundColor(Color.parseColor("#CCCCCC"));
                linhaInf.setBackgroundColor(Color.parseColor("#CCCCCC"));
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), DetalhesAgendamentoActivity.class);
                intent.putExtra("agendamento", ag);
                startActivity(intent);
            });

            containerAgendamentos.addView(itemView);
        }
    }
}