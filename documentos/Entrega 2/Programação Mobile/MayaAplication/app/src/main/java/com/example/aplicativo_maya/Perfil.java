package com.example.aplicativo_maya;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Perfil extends AppCompatActivity {

    private static final String TAG = "PerfilActivity";
    private static final String API_BASE_URL = "https://maya-rpg-api-ckx5.onrender.com/api";

    private TextView tvNomePaciente, tvStatusSincronizacao;
    private TextView tvTotalExercicios, tvMediaDor, tvOfensiva;
    private LineChart lineChartEvolucao;
    private RecyclerView rvHistoricoCheckins;
    private CheckinAdapter checkinAdapter;
    private List<JSONObject> ultimosCheckins = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        tvNomePaciente = findViewById(R.id.tvNomePaciente);
        tvStatusSincronizacao = findViewById(R.id.tvStatusSincronizacao);
        tvTotalExercicios = findViewById(R.id.tvTotalExercicios);
        tvMediaDor = findViewById(R.id.tvMediaDor);
        tvOfensiva = findViewById(R.id.tvOfensiva);
        lineChartEvolucao = findViewById(R.id.lineChartEvolucao);
        rvHistoricoCheckins = findViewById(R.id.rvHistoricoCheckins);

        btnVoltar.setOnClickListener(v -> finish());


        rvHistoricoCheckins.setLayoutManager(new LinearLayoutManager(this));
        checkinAdapter = new CheckinAdapter(ultimosCheckins);
        rvHistoricoCheckins.setAdapter(checkinAdapter);

        setupChartAppearance();

        carregarDadosDoPerfil();
    }

    private void setupChartAppearance() {
        lineChartEvolucao.getDescription().setEnabled(false);
        lineChartEvolucao.setTouchEnabled(true);
        lineChartEvolucao.setDragEnabled(true);
        lineChartEvolucao.setScaleEnabled(false);
        lineChartEvolucao.getAxisRight().setEnabled(false);

        XAxis xAxis = lineChartEvolucao.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#666666"));

        lineChartEvolucao.getAxisLeft().setAxisMinimum(0f);
        lineChartEvolucao.getAxisLeft().setAxisMaximum(10f);
        lineChartEvolucao.getAxisLeft().setTextColor(Color.parseColor("#666666"));
    }

    private void carregarDadosDoPerfil() {
        SharedPreferences prefs = getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("JWT_TOKEN", null);

        if (token == null) {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String response = fazerRequisicaoGET("/paciente/me", token);
                JSONObject user = new JSONObject(response);
                String nome = user.optString("name", "Paciente");

                mainHandler.post(() -> tvNomePaciente.setText("Olá, " + nome.split(" ")[0] + "!"));
            } catch (Exception e) { Log.e(TAG, "Erro /me", e); }
        });

        executor.execute(() -> {
            try {
                String response = fazerRequisicaoGET("/paciente/checkins/evolucao", token);
                JSONArray evolucoes = new JSONArray(response);

                List<Entry> chartEntries = new ArrayList<>();
                List<String> xLabels = new ArrayList<>();
                int totalExercicios = 0;
                float somaDor = 0f;
                int diasComDor = 0;

                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM", Locale.getDefault());

                for (int i = 0; i < evolucoes.length(); i++) {
                    JSONObject evo = evolucoes.getJSONObject(i);
                    String dataOriginal = evo.optString("data");
                    float dor = (float) evo.optDouble("mediaNivelDor", 0);
                    int concluidos = evo.optInt("totalExerciciosConcluidos", 0);

                    totalExercicios += concluidos;
                    if (dor > 0) { somaDor += dor; diasComDor++; }

                    try {
                        Date date = sdfInput.parse(dataOriginal);
                        xLabels.add(sdfOutput.format(date));
                    } catch (Exception e) {
                        xLabels.add(dataOriginal.substring(5));
                    }
                    chartEntries.add(new Entry(i, dor));
                }

                final String fTotal = String.valueOf(totalExercicios);
                final String fMedia = diasComDor > 0 ? String.format(Locale.US, "%.1f", (somaDor / diasComDor)) : "0.0";
                final String fOfensiva = String.valueOf(evolucoes.length());

                mainHandler.post(() -> {
                    tvTotalExercicios.setText(fTotal);
                    tvMediaDor.setText(fMedia);
                    tvOfensiva.setText(fOfensiva);

                    atualizarGrafico(chartEntries, xLabels);
                });

            } catch (Exception e) { Log.e(TAG, "Erro /evolucao", e); }
        });

        executor.execute(() -> {
            try {
                String response = fazerRequisicaoGET("/paciente/checkins", token);
                JSONArray checkins = new JSONArray(response);

                List<JSONObject> parseados = new ArrayList<>();

                int max = Math.min(checkins.length(), 10);
                for (int i = 0; i < max; i++) {
                    parseados.add(checkins.getJSONObject(i));
                }

                mainHandler.post(() -> {
                    ultimosCheckins.clear();
                    ultimosCheckins.addAll(parseados);
                    checkinAdapter.notifyDataSetChanged();

                    SimpleDateFormat sdfHorario = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    tvStatusSincronizacao.setText("Sincronizado hoje às " + sdfHorario.format(new Date()));
                });

            } catch (Exception e) { Log.e(TAG, "Erro /checkins", e); }
        });
    }

    private void atualizarGrafico(List<Entry> entries, List<String> xLabels) {
        if (entries.isEmpty()) {
            lineChartEvolucao.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Média de Dor");
        dataSet.setColor(Color.parseColor("#FF7560"));
        dataSet.setCircleColor(Color.parseColor("#FF7560"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FF7560"));
        dataSet.setFillAlpha(40);

        LineData lineData = new LineData(dataSet);
        lineChartEvolucao.setData(lineData);

        lineChartEvolucao.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

        lineChartEvolucao.animateX(1000);
        lineChartEvolucao.invalidate();
    }

    private String fazerRequisicaoGET(String endpoint, String token) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        if (conn.getResponseCode() == 200) {
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) res.append(line);
            r.close();
            conn.disconnect();
            return res.toString();
        } else {
            conn.disconnect();
            throw new Exception("Erro HTTP: " + conn.getResponseCode());
        }
    }

    private class CheckinAdapter extends RecyclerView.Adapter<CheckinAdapter.ViewHolder> {

        private List<JSONObject> items;
        private SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        private SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

        CheckinAdapter(List<JSONObject> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject checkin = items.get(position);
            String titulo = checkin.optString("exercicioTitulo", "Exercício concluído");
            int dor = checkin.optInt("nivelDor", 0);
            String dataStr = checkin.optString("dataExecucao");

            String dataFormatada = dataStr;
            try {
                Date d = sdfInput.parse(dataStr);
                dataFormatada = sdfOutput.format(d);
            } catch (Exception ignored) {}

            holder.text1.setText(titulo);
            holder.text1.setTextColor(Color.parseColor("#333333"));
            holder.text1.setTextSize(16f);

            holder.text2.setText("Data: " + dataFormatada + "  •  Dor reportada: " + dor);
            holder.text2.setTextColor(Color.parseColor("#888888"));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}