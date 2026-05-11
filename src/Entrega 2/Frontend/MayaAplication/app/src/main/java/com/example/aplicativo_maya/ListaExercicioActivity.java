package com.example.aplicativo_maya;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaExercicioActivity extends AppCompatActivity {

    private static final String TAG = "ListaExercicio";

    private RecyclerView recyclerExercicios;
    private ProgressBar progressBar;
    private ExercicioAdapter adapter;
    private List<ExercicioPrescrito> lista = new ArrayList<>();
    private final String API_BASE_URL = "https://maya-rpg-api-ckx5.onrender.com/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_exercicio);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        TextView txtTituloRotina = findViewById(R.id.txtTituloRotina);
        recyclerExercicios = findViewById(R.id.recyclerExercicios);
        progressBar = findViewById(R.id.progressBar);

        ImageView bg = findViewById(R.id.imageViewBackground);
        ImageView overlay = findViewById(R.id.imageViewOverlay);
        Glide.with(this).load(R.drawable.fundolisexe).centerCrop().into(bg);
        Glide.with(this).load(R.drawable.background_login).into(overlay);

        long rotinaId = getIntent().getLongExtra("ROTINA_ID", -1);
        if (rotinaId == -1) rotinaId = (long) getIntent().getIntExtra("ROTINA_ID", -1);

        String rotinaNome = getIntent().getStringExtra("ROTINA_NOME");
        if (rotinaNome != null) txtTituloRotina.setText(rotinaNome);

        btnVoltar.setOnClickListener(v -> finish());

        recyclerExercicios.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExercicioAdapter(lista, this);
        recyclerExercicios.setAdapter(adapter);

        if (rotinaId != -1) {
            carregarExercicios(rotinaId);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Erro: Rotina não encontrada", Toast.LENGTH_SHORT).show();
        }
    }

    private void carregarExercicios(long rotinaId) {
        SharedPreferences prefs = getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("JWT_TOKEN", null);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL url = new URL(API_BASE_URL + "/paciente/rotinas/" + rotinaId + "/exercicios");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    List<ExercicioPrescrito> baixados = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        ExercicioPrescrito ex = new ExercicioPrescrito();
                        ex.id = obj.optLong("id");
                        ex.rotinaId = obj.optLong("rotinaId", rotinaId);
                        ex.series = obj.optInt("series", obj.optInt("qtdSeries", 0));
                        ex.repeticoes = obj.optInt("repeticoes", obj.optInt("qtdRepeticoes", 0));
                        ex.tempoSegundos = obj.optInt("tempoSegundos", obj.optInt("tempo", 0));

                        JSONObject inner = obj.optJSONObject("exercicio");
                        if (inner == null) inner = obj.optJSONObject("exercicioPrescrito");
                        if (inner == null) inner = obj.optJSONObject("exercicioBase");

                        String titulo = primeiroValido(
                                obj.optString("titulo"), obj.optString("nome"), obj.optString("exercicioTitulo"),
                                inner != null ? inner.optString("titulo") : null,
                                inner != null ? inner.optString("nome") : null
                        );
                        ex.exercicioTitulo = isValido(titulo) ? titulo : "Exercício";

                        String desc = obj.optString("descricao");
                        if (!isValido(desc)) desc = obj.optString("descrição");
                        if (!isValido(desc)) desc = obj.optString("observacoes");
                        if (!isValido(desc)) desc = obj.optString("observacao");

                        if (!isValido(desc) && inner != null) {
                            desc = inner.optString("descricao");
                            if (!isValido(desc)) desc = inner.optString("descrição");
                            if (!isValido(desc)) desc = inner.optString("observacoes");
                        }

                        if (!isValido(desc)) desc = buscarTextoAgressivo(obj);
                        if (!isValido(desc) && inner != null) desc = buscarTextoAgressivo(inner);

                        if (!isValido(desc)) {
                            desc = "";
                        }

                        ex.observacoes = desc;

                        String videoUrl = primeiroValido(
                                obj.optString("videoUrl"), obj.optString("linkVideo"),
                                inner != null ? inner.optString("videoUrl") : null
                        );
                        ex.videoUrl = isValido(videoUrl) ? videoUrl : "";

                        String fotoUrl = primeiroValido(
                                obj.optString("fotoUrl"), obj.optString("imagemUrl"),
                                inner != null ? inner.optString("fotoUrl") : null
                        );
                        ex.fotoUrl = isValido(fotoUrl) ? fotoUrl : "";

                        baixados.add(ex);
                    }

                    handler.post(() -> {
                        lista.clear();
                        lista.addAll(baixados);
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        recyclerExercicios.setVisibility(View.VISIBLE);
                        verificarConclusaoDaRotina();
                    });
                } else {
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Erro HTTP " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> progressBar.setVisibility(View.GONE));
            }
        });
    }

    private String buscarTextoAgressivo(JSONObject json) {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String lowerKey = key.toLowerCase();
            if (lowerKey.contains("desc") || lowerKey.contains("obs") || lowerKey.contains("detalhe")) {
                String val = json.optString(key);
                if (isValido(val)) return val;
            }
        }
        return null;
    }

    private String primeiroValido(String... valores) {
        for (String v : valores) {
            if (isValido(v)) return v;
        }
        return null;
    }

    private boolean isValido(String s) {
        return s != null && !s.trim().isEmpty() && !s.trim().equalsIgnoreCase("null");
    }

    private void verificarConclusaoDaRotina() {
        if (lista.isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
        boolean todosConcluidos = true;

        for (ExercicioPrescrito ex : lista) {
            if (!prefs.getBoolean("CONCLUIDO_" + ex.id, false)) {
                todosConcluidos = false;
                break;
            }
        }

        long rotinaId = getIntent().getLongExtra("ROTINA_ID", -1);
        if (rotinaId == -1) rotinaId = (long) getIntent().getIntExtra("ROTINA_ID", -1);

        boolean jaEstavaConcluida = prefs.getBoolean("ROTINA_CONCLUIDA_" + rotinaId, false);

        if (todosConcluidos && !jaEstavaConcluida && rotinaId != -1) {
            prefs.edit().putBoolean("ROTINA_CONCLUIDA_" + rotinaId, true).apply();
            Toast.makeText(this, "Parabéns! Rotina Finalizada!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            verificarConclusaoDaRotina();
        }
    }
}