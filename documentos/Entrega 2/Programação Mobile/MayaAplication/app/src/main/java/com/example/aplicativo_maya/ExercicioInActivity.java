package com.example.aplicativo_maya;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExercicioInActivity extends AppCompatActivity {

    private static final String TAG = "ExercicioIn";
    private static final String IMAGE_BASE_URL = "https://maya-rpg-api-ckx5.onrender.com";

    private Long exercicioPrescritoId;
    private long rotinaId = -1L;
    private final String API_BASE_URL = "https://maya-rpg-api-ckx5.onrender.com/api";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FrameLayout videoContainer;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        setContentView(R.layout.exercicio_in);

        ImageButton  btnVoltar          = findViewById(R.id.btnVoltar);
        TextView     txtTituloExercicio = findViewById(R.id.txtTituloExercicio);
        TextView     txtSeries          = findViewById(R.id.txtSeries);
        TextView     txtMinutos         = findViewById(R.id.txtMinutos);
        TextView     txtSeparador       = findViewById(R.id.txtSeparador);
        Button       btnCheckIn         = findViewById(R.id.btnCheckIn);
        ImageView    imgFixedTop        = findViewById(R.id.imgFixedTop);
        ProgressBar  progressLoading    = findViewById(R.id.progressLoading);
        LinearLayout conteudoExercicio  = findViewById(R.id.conteudoExercicio);
        TextView     txtDescricao       = findViewById(R.id.txtDescricao);
        ImageView    imgReferencia      = findViewById(R.id.imgReferenciaExercicio);
        videoContainer                  = findViewById(R.id.videoContainer);

        Glide.with(this).load(R.drawable.exercicio_in).centerCrop().into(imgFixedTop);

        exercicioPrescritoId = getIntent().getLongExtra("ID_PRESCRICAO", -1L);
        if (exercicioPrescritoId == -1L)
            exercicioPrescritoId = (long) getIntent().getIntExtra("ID_PRESCRICAO", -1);

        rotinaId = getIntent().getLongExtra("ROTINA_ID", -1L);

        String titulo      = getIntent().getStringExtra("TITULO");
        int    series      = getIntent().getIntExtra("SERIES", 0);
        int    repeticoes  = getIntent().getIntExtra("REPETICOES", 0);
        int    tempo       = getIntent().getIntExtra("TEMPO", 0);

        // Recebe o texto (descricao) capturado de forma dinâmica na ListaExercicioActivity
        String textoExibicao = getIntent().getStringExtra("OBSERVACOES");

        String videoUrl    = getIntent().getStringExtra("VIDEO_URL");
        String fotoUrl     = normalizarUrlImagem(getIntent().getStringExtra("FOTO_URL"));

        overridePendingTransition(0, 0);

        txtTituloExercicio.setText(isValido(titulo) ? titulo : "Exercício");
        txtSeries.setText("Séries: " + series + "x");

        if (tempo > 0)           txtMinutos.setText("Tempo: " + tempo + "s");
        else if (repeticoes > 0) txtMinutos.setText("Reps: " + repeticoes);
        else {
            txtMinutos.setVisibility(View.GONE);
            txtSeparador.setVisibility(View.GONE);
        }

        btnVoltar.setOnClickListener(v -> finish());
        btnCheckIn.setOnClickListener(v -> abrirPopupCheckin());

        final String fotoFinal  = fotoUrl;
        final String videoFinal = videoUrl;

        mainHandler.postDelayed(() -> {

            progressLoading.setVisibility(View.GONE);
            conteudoExercicio.setVisibility(View.VISIBLE);

            SharedPreferences prefs = getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
            btnCheckIn.setVisibility(
                    prefs.getBoolean("CONCLUIDO_" + exercicioPrescritoId, false)
                            ? View.GONE : View.VISIBLE);

            txtDescricao.setVisibility(View.VISIBLE);

            // APLICA O TEXTO DINÂMICO! Se der erro, você verá o JSON cru impresso aqui.
            txtDescricao.setText(isValido(textoExibicao)
                    ? textoExibicao
                    : "Realize o exercício conforme orientado pelo seu fisioterapeuta.");

            if (isValidUrl(fotoFinal)) {
                imgReferencia.setVisibility(View.VISIBLE);
                Glide.with(ExercicioInActivity.this)
                        .load(fotoFinal)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.circulo_vermelho)
                        .error(R.drawable.circulo_vermelho)
                        .into(imgReferencia);
            } else {
                imgReferencia.setVisibility(View.GONE);
            }

            if (isValidUrl(videoFinal)) {
                String videoId = extrairIDdoYoutube(videoFinal);
                if (videoId != null) {
                    carregarVideoYoutube(videoId, videoFinal);
                } else {
                    videoContainer.setVisibility(View.GONE);
                }
            } else {
                videoContainer.setVisibility(View.GONE);
            }

        }, 400);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void carregarVideoYoutube(String videoId, String videoUrl) {
        videoContainer.removeAllViews();
        videoContainer.setVisibility(View.VISIBLE);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(
                new YoutubePlayerBridge(videoId, videoUrl), "AndroidBridge");

        String html = "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "  * { margin:0; padding:0; box-sizing:border-box; }"
                + "  body { background:#000; overflow:hidden; }"
                + "  #player { width:100%; height:100vh; }"
                + "</style>"
                + "</head><body>"
                + "<div id='player'></div>"
                + "<script>"
                + "  var tag = document.createElement('script');"
                + "  tag.src = 'https://www.youtube.com/iframe_api';"
                + "  document.head.appendChild(tag);"
                + ""
                + "  var player;"
                + "  function onYouTubeIframeAPIReady() {"
                + "    player = new YT.Player('player', {"
                + "      videoId: '" + videoId + "',"
                + "      playerVars: {"
                + "        playsinline: 1,"
                + "        rel: 0,"
                + "        modestbranding: 1"
                + "      },"
                + "      events: {"
                + "        onError: function(event) {"
                + "          AndroidBridge.onPlayerError(event.data);"
                + "        }"
                + "      }"
                + "    });"
                + "  }"
                + "</script>"
                + "</body></html>";

        webView.loadDataWithBaseURL(
                "https://www.youtube.com",
                html,
                "text/html",
                "UTF-8",
                null
        );

        videoContainer.addView(webView);
    }

    private class YoutubePlayerBridge {
        private final String videoId;
        private final String videoUrl;

        YoutubePlayerBridge(String videoId, String videoUrl) {
            this.videoId  = videoId;
            this.videoUrl = videoUrl;
        }

        @JavascriptInterface
        public void onPlayerError(int errorCode) {
            Log.w(TAG, "YouTube player error code: " + errorCode);
            mainHandler.post(() -> mostrarFallbackYoutube(videoId, videoUrl, errorCode));
        }
    }

    private void mostrarFallbackYoutube(String videoId, String videoUrl, int errorCode) {
        videoContainer.removeAllViews();

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView thumbnail = new ImageView(this);
        thumbnail.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(200)));
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setBackgroundColor(Color.parseColor("#1A1A1A"));

        Glide.with(this)
                .load("https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(thumbnail);

        Button btnYoutube = new Button(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = dpToPx(8);
        btnYoutube.setLayoutParams(btnParams);
        btnYoutube.setText("▶  Assistir no YouTube");
        btnYoutube.setTextColor(Color.WHITE);
        btnYoutube.setBackgroundColor(Color.parseColor("#FF0000"));
        btnYoutube.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        btnYoutube.setAllCaps(false);
        btnYoutube.setTextSize(15f);

        String motivo = (errorCode == 101 || errorCode == 150)
                ? "O dono deste vídeo não permite reprodução em outros apps."
                : "Este vídeo não pôde ser carregado no app.";
        TextView txtNota = new TextView(this);
        LinearLayout.LayoutParams notaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        notaParams.topMargin = dpToPx(6);
        txtNota.setLayoutParams(notaParams);
        txtNota.setText(motivo);
        txtNota.setTextColor(Color.parseColor("#999999"));
        txtNota.setTextSize(12f);

        View.OnClickListener abrir = v -> abrirYoutube(videoUrl);
        thumbnail.setOnClickListener(abrir);
        thumbnail.setClickable(true);
        btnYoutube.setOnClickListener(abrir);

        wrapper.addView(thumbnail);
        wrapper.addView(btnYoutube);
        wrapper.addView(txtNota);
        videoContainer.addView(wrapper);
    }

    private void abrirYoutube(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.google.android.youtube");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o vídeo.", Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizarUrlImagem(String url) {
        if (!isValido(url)) return null;
        url = url.trim();
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        if (url.startsWith("data:image")) return url;
        if (url.startsWith("/")) return IMAGE_BASE_URL + url;
        return IMAGE_BASE_URL + "/uploads/" + url;
    }

    private boolean isValido(String s) {
        return s != null && !s.trim().isEmpty() && !s.trim().equalsIgnoreCase("null");
    }

    private boolean isValidUrl(String url) {
        return isValido(url) && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:image"));
    }

    private String extrairIDdoYoutube(String url) {
        try {
            if (url.contains("v=")) {
                String id = url.split("v=")[1];
                if (id.contains("&")) id = id.split("&")[0];
                return id.isEmpty() ? null : id;
            } else if (url.contains("youtu.be/")) {
                String id = url.split("youtu.be/")[1];
                if (id.contains("?")) id = id.split("\\?")[0];
                return id.isEmpty() ? null : id;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro extraindo ID YouTube", e);
        }
        return null;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void abrirPopupCheckin() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_checkin);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        SeekBar  seekBarDor       = dialog.findViewById(R.id.seekBarDor);
        TextView txtNivelDorLabel = dialog.findViewById(R.id.txtNivelDorLabel);
        EditText edtObservacao    = dialog.findViewById(R.id.edtObservacao);
        Button   btnConfirmar     = dialog.findViewById(R.id.btnConfirmarCheckin);

        seekBarDor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                txtNivelDorLabel.setText("Nível de Dor: " + p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        btnConfirmar.setOnClickListener(v -> {
            enviarCheckInParaBanco(seekBarDor.getProgress(),
                    edtObservacao.getText().toString());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void enviarCheckInParaBanco(int dor, String obs) {
        if (exercicioPrescritoId == null || exercicioPrescritoId == -1L) return;

        SharedPreferences prefs = getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("JWT_TOKEN", null);

        prefs.edit().putBoolean("CONCLUIDO_" + exercicioPrescritoId, true).apply();
        findViewById(R.id.btnCheckIn).setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("nivelDor", dor);
                jsonBody.put("observacao", obs);
                jsonBody.put("concluido", true);
                String dataAtual = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                        Locale.getDefault()).format(new Date());
                jsonBody.put("dataExecucao", dataAtual);

                URL url = new URL(API_BASE_URL + "/paciente/exercicios/"
                        + exercicioPrescritoId + "/checkin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                mainHandler.post(() -> {
                    if (code == 200 || code == 201) {
                        try {
                            String histStr = prefs.getString("HISTORICO_EXERCICIOS", "[]");
                            JSONArray hist = new JSONArray(histStr);
                            JSONObject novoEx = new JSONObject();
                            novoEx.put("id", exercicioPrescritoId);
                            novoEx.put("data", dataAtual);
                            JSONArray novoHist = new JSONArray();
                            novoHist.put(novoEx);
                            int max = Math.min(hist.length(), 9);
                            for (int i = 0; i < max; i++)
                                novoHist.put(hist.getJSONObject(i));
                            prefs.edit().putString("HISTORICO_EXERCICIOS", novoHist.toString()).apply();

                            if (rotinaId != -1L) {
                                int concluidosNaRotina = prefs.getInt("CONCLUIDOS_ROTINA_" + rotinaId, 0);
                                prefs.edit().putInt("CONCLUIDOS_ROTINA_" + rotinaId, concluidosNaRotina + 1).apply();
                            }

                        } catch (Exception ignored) {}
                    }
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Erro no check-in", e);
                mainHandler.post(() -> finish());
            } finally {
                executor.shutdown();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);

        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
            webView.removeAllViews();
            webView.clearHistory();
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}