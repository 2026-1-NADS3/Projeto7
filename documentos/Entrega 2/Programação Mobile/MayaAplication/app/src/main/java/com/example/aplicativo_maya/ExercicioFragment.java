package com.example.aplicativo_maya;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExercicioFragment extends Fragment {

    private RecyclerView recyclerViewExercicios;
    private SwipeRefreshLayout swipeRefreshLayout;
    private GrupoExercicioAdapter adapter;
    private List<Rotina> listaDeRotinas = new ArrayList<>();
    private final String API_BASE_URL = "https://maya-rpg-api-ckx5.onrender.com/api";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_exercicio, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerViewExercicios = view.findViewById(R.id.recyclerViewExercicios);
        recyclerViewExercicios.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new GrupoExercicioAdapter(listaDeRotinas, getContext());
        recyclerViewExercicios.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> carregarRotinasDoServidor(getContext()));
        swipeRefreshLayout.setRefreshing(true);
        carregarRotinasDoServidor(getContext());

        return view;
    }

    private void carregarRotinasDoServidor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
        String tokenSalvo = prefs.getString("JWT_TOKEN", null);

        if (tokenSalvo == null) {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(false);
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL urlProntuario = new URL(API_BASE_URL + "/paciente/prontuarios");
                HttpURLConnection conn1 = (HttpURLConnection) urlProntuario.openConnection();
                conn1.setRequestMethod("GET");
                conn1.setRequestProperty("Authorization", "Bearer " + tokenSalvo);

                if (conn1.getResponseCode() == 200) {
                    BufferedReader r1 = new BufferedReader(new InputStreamReader(conn1.getInputStream(), "UTF-8"));
                    StringBuilder res1 = new StringBuilder();
                    String line1; while ((line1 = r1.readLine()) != null) res1.append(line1); r1.close();

                    JSONArray prontuarios = new JSONArray(res1.toString());
                    List<Rotina> ativas = new ArrayList<>();
                    List<Rotina> completas = new ArrayList<>();

                    for (int p = 0; p < prontuarios.length(); p++) {
                        int prontuarioId = prontuarios.getJSONObject(p).getInt("id");

                        URL urlRotina = new URL(API_BASE_URL + "/paciente/prontuarios/" + prontuarioId + "/rotinas");
                        HttpURLConnection conn2 = (HttpURLConnection) urlRotina.openConnection();
                        conn2.setRequestMethod("GET");
                        conn2.setRequestProperty("Authorization", "Bearer " + tokenSalvo);

                        if (conn2.getResponseCode() == 200) {
                            BufferedReader r2 = new BufferedReader(new InputStreamReader(conn2.getInputStream(), "UTF-8"));
                            StringBuilder res2 = new StringBuilder();
                            String line2; while ((line2 = r2.readLine()) != null) res2.append(line2); r2.close();

                            JSONArray rotinasJson = new JSONArray(res2.toString());
                            for (int i = 0; i < rotinasJson.length(); i++) {
                                JSONObject obj = rotinasJson.getJSONObject(i);
                                Rotina r = new Rotina();
                                r.id = obj.optInt("id", 0);
                                r.nome = obj.optString("nome", "Treino");
                                r.totalExercicios = obj.optInt("totalExercicios", 0);

                                boolean rotinaCompleta = prefs.getBoolean("ROTINA_CONCLUIDA_" + r.id, false);
                                if (!rotinaCompleta && r.totalExercicios > 0) {
                                    try {
                                        URL urlEx = new URL(API_BASE_URL + "/paciente/rotinas/" + r.id + "/exercicios");
                                        HttpURLConnection conn3 = (HttpURLConnection) urlEx.openConnection();
                                        conn3.setRequestMethod("GET");
                                        conn3.setRequestProperty("Authorization", "Bearer " + tokenSalvo);

                                        if (conn3.getResponseCode() == 200) {
                                            BufferedReader r3 = new BufferedReader(new InputStreamReader(conn3.getInputStream(), "UTF-8"));
                                            StringBuilder res3 = new StringBuilder();
                                            String line3; while ((line3 = r3.readLine()) != null) res3.append(line3); r3.close();

                                            JSONArray exerciciosJson = new JSONArray(res3.toString());
                                            int concluidos = 0;

                                            for (int j = 0; j < exerciciosJson.length(); j++) {
                                                int exId = exerciciosJson.getJSONObject(j).getInt("id");
                                                if (prefs.getBoolean("CONCLUIDO_" + exId, false)) {
                                                    concluidos++;
                                                }
                                            }

                                            if (concluidos == r.totalExercicios) {
                                                rotinaCompleta = true;
                                                prefs.edit().putBoolean("ROTINA_CONCLUIDA_" + r.id, true).apply();
                                            }
                                        }
                                    } catch (Exception ignored) {

                                    }
                                }


                                if (rotinaCompleta) {
                                    completas.add(r);
                                } else {
                                    ativas.add(r);
                                }
                            }
                        }
                    }

                    handler.post(() -> {
                        listaDeRotinas.clear();
                        listaDeRotinas.addAll(ativas);

                        if (!completas.isEmpty()) {
                            Rotina separador = new Rotina();
                            separador.id = -1;
                            listaDeRotinas.add(separador);
                            listaDeRotinas.addAll(completas);
                        }

                        adapter.notifyDataSetChanged();
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                } else {
                    handler.post(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) carregarRotinasDoServidor(getContext());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
            if (adapter != null) {
                carregarRotinasDoServidor(getContext());
            }
        }
    }
}