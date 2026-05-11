package com.example.aplicativo_maya;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private FooterNavigationView footer;
    private HamburgerMenuView hamburgerMenu;
    private SessionManager session;
    private static final String[] TAGS = {"home", "exercicio", "agendamento", "mural"};
    private int currentIndex = 0;
    private ChildEventListener notificacoesListener;
    private Query chatsQuery;
    private Handler radarHandler = new Handler(Looper.getMainLooper());
    private ExecutorService radarExecutor = Executors.newSingleThreadExecutor();
    private Runnable radarRunnable = new Runnable() {
        @Override
        public void run() {
            verificarNovasRotinas();
            radarHandler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.estaLogado()) {
            irParaLogin();
            return;
        }

        setContentView(R.layout.activity_home);

        // Permissão de notificações
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Token FCM
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM_TOKEN", "Falha ao obter token do Firebase", task.getException());
                        return;
                    }
                    Log.d("FCM_TOKEN", "SEU TOKEN DE NOTIFICACAO: " + task.getResult());
                });

        footer        = findViewById(R.id.footer_navigation);
        hamburgerMenu = findViewById(R.id.meu_hamburger);

        if (savedInstanceState == null) {
            Fragment home         = new HomepageFragment();
            Fragment exercicios   = new ExercicioFragment();
            Fragment agendamentos = new AgendamentosFragment();
            Fragment chat         = new ChatFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, home,         TAGS[0])
                    .add(R.id.fragment_container, exercicios,   TAGS[1]).hide(exercicios)
                    .add(R.id.fragment_container, agendamentos, TAGS[2]).hide(agendamentos)
                    .add(R.id.fragment_container, chat,         TAGS[3]).hide(chat)
                    .commitNow();
        }

        footer.setSelectedTab(0);
        footer.setOnTabSelectedListener(this::showFragment);

        if (hamburgerMenu != null) {
            hamburgerMenu.setOnMenuItemClickListener(viewId -> {
                if (viewId == R.id.menu_item_perfil) {
                    startActivity(new Intent(MainActivity.this, Perfil.class));
                } else if (viewId == R.id.menu_item_logout) {
                    confirmarLogout();
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(footer, (view, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bottom);
            return insets;
        });

        iniciarEscutaDeNotificacoesGlobais();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (session != null && session.estaLogado()) {
            radarHandler.post(radarRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        radarHandler.removeCallbacks(radarRunnable);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.getBooleanExtra("ABRIR_ABA_EXERCICIOS", false)) {
            showFragment(1);
        }
    }


    private void verificarNovasRotinas() {
        String tokenSalvo = session.getToken();
        if (tokenSalvo == null || tokenSalvo.isEmpty()) return;

        radarExecutor.execute(() -> {
            try {
                String API_BASE_URL = "https://maya-rpg-api-ckx5.onrender.com/api";

                java.net.URL urlProntuario = new java.net.URL(API_BASE_URL + "/paciente/prontuarios");
                java.net.HttpURLConnection conn1 = (java.net.HttpURLConnection) urlProntuario.openConnection();
                conn1.setRequestMethod("GET");
                conn1.setRequestProperty("Authorization", "Bearer " + tokenSalvo);

                if (conn1.getResponseCode() == 200) {
                    java.io.BufferedReader r1 = new java.io.BufferedReader(new java.io.InputStreamReader(conn1.getInputStream(), "UTF-8"));
                    StringBuilder res1 = new StringBuilder();
                    String line1; while ((line1 = r1.readLine()) != null) res1.append(line1); r1.close();

                    org.json.JSONArray prontuarios = new org.json.JSONArray(res1.toString());

                    android.content.SharedPreferences prefs = getSharedPreferences("MayaAppPrefs", android.content.Context.MODE_PRIVATE);
                    String rotinasConhecidas = prefs.getString("ROTINAS_NOTIFICADAS", "");

                    boolean isPrimeiraVez = rotinasConhecidas.isEmpty();
                    boolean teveNova = false;

                    for (int p = 0; p < prontuarios.length(); p++) {
                        int prontuarioId = prontuarios.getJSONObject(p).getInt("id");

                        java.net.URL urlRotina = new java.net.URL(API_BASE_URL + "/paciente/prontuarios/" + prontuarioId + "/rotinas");
                        java.net.HttpURLConnection conn2 = (java.net.HttpURLConnection) urlRotina.openConnection();
                        conn2.setRequestMethod("GET");
                        conn2.setRequestProperty("Authorization", "Bearer " + tokenSalvo);

                        if (conn2.getResponseCode() == 200) {
                            java.io.BufferedReader r2 = new java.io.BufferedReader(new java.io.InputStreamReader(conn2.getInputStream(), "UTF-8"));
                            StringBuilder res2 = new StringBuilder();
                            String line2; while ((line2 = r2.readLine()) != null) res2.append(line2); r2.close();

                            org.json.JSONArray rotinasJson = new org.json.JSONArray(res2.toString());
                            for (int i = 0; i < rotinasJson.length(); i++) {
                                org.json.JSONObject obj = rotinasJson.getJSONObject(i);
                                int rotinaId = obj.optInt("id", 0);
                                String nome = obj.optString("nome", "Treino");

                                if (!rotinasConhecidas.contains("[" + rotinaId + "]")) {

                                    if (!isPrimeiraVez) {
                                        NotificacaoHelper.dispararNotificacaoRotina(MainActivity.this, nome, rotinaId);
                                    }

                                    rotinasConhecidas += "[" + rotinaId + "]";
                                    teveNova = true;
                                }
                            }
                        }
                    }

                    if (teveNova) {
                        prefs.edit().putString("ROTINAS_NOTIFICADAS", rotinasConhecidas).apply();
                    }
                }
            } catch (Exception ignored) {

            }
        });
    }


    private void iniciarEscutaDeNotificacoesGlobais() {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        chatsQuery = chatsRef.orderByChild("pacienteId").equalTo(session.getPacienteId());

        notificacoesListener = new ChildEventListener() {
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String ticketId = snapshot.child("id").getValue(String.class);
                String titulo = snapshot.child("titulo").getValue(String.class);
                Integer naoLidas = snapshot.child("naoLidasPaciente").getValue(Integer.class);

                if (naoLidas != null && naoLidas > 0 && !ticketId.equals(ChatTicketActivity.ticketAbertoAtual)) {
                    NotificacaoHelper.disparar(
                            MainActivity.this,
                            titulo,
                            "Você tem " + naoLidas + " nova(s) mensagem(ns).",
                            ticketId
                    );
                }
            }

            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        chatsQuery.addChildEventListener(notificacoesListener);
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da conta")
                .setMessage("Tem certeza que deseja sair?")
                .setPositiveButton("Sair", (dialog, which) -> fazerLogout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void fazerLogout() {
        session.encerrarSessao();
        irParaLogin();
    }

    private void irParaLogin() {
        Intent intent = new Intent(this, Login_Maya.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void showFragment(int index) {
        if (index == currentIndex) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(TAGS[currentIndex]);
        Fragment nextFragment    = getSupportFragmentManager().findFragmentByTag(TAGS[index]);

        if (currentFragment != null) ft.hide(currentFragment);
        if (nextFragment    != null) ft.show(nextFragment);

        ft.commit();
        currentIndex = index;
        footer.setSelectedTab(index);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        radarExecutor.shutdown();
        if (chatsQuery != null && notificacoesListener != null) {
            chatsQuery.removeEventListener(notificacoesListener);
        }
    }
}