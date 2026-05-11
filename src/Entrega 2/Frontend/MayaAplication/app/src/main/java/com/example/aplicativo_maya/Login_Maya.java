package com.example.aplicativo_maya;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicativo_maya.api.AuthResponse;
import com.example.aplicativo_maya.api.CheckinResponse;
import com.example.aplicativo_maya.api.LoginRequest;
import com.example.aplicativo_maya.api.MayaRpgApi;
import com.example.aplicativo_maya.api.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login_Maya extends AppCompatActivity {

    private EditText txbEmail;
    private EditText txbSenha;
    private Button   btnLogar;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        if (session.estaLogado()) {
            setContentView(R.layout.activity_login_maya);
            mapearComponentes();
            btnLogar.setEnabled(false);
            btnLogar.setText("Sincronizando...");
            sincronizarCheckins(session.getToken(), this);
            return;
        }

        setContentView(R.layout.activity_login_maya);
        mapearComponentes();
    }

    private void mapearComponentes() {
        txbEmail = findViewById(R.id.txbEmail);
        txbSenha = findViewById(R.id.txbSenha);
        btnLogar = findViewById(R.id.btnLogar);
        TextView textoCadastrar = findViewById(R.id.txtCadastrar);

        btnLogar.setOnClickListener(v -> fazerLogin());
        if (textoCadastrar != null) {
            textoCadastrar.setOnClickListener(v -> {
                startActivity(new Intent(Login_Maya.this, cadastro.class));
            });
        }
    }

    private void fazerLogin() {
        String email = txbEmail.getText().toString().trim();
        String senha = txbSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogar.setEnabled(false);
        btnLogar.setText("Aguarde...");

        MayaRpgApi api = RetrofitClient.getInstance().create(MayaRpgApi.class);
        api.login(new LoginRequest(email, senha)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse dados = response.body();
                    session.salvarSessao(dados.getToken(), dados.getId(), dados.getName(), dados.getEmail());
                    sincronizarCheckins(dados.getToken(), Login_Maya.this);
                } else {
                    btnLogar.setEnabled(true);
                    btnLogar.setText("Entrar");
                    Toast.makeText(Login_Maya.this, "Erro no login.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnLogar.setEnabled(true);
                btnLogar.setText("Entrar");
            }
        });
    }

    private void sincronizarCheckins(String token, Context context) {
        MayaRpgApi api = RetrofitClient.getInstance().create(MayaRpgApi.class);
        api.getMeusCheckins("Bearer " + token).enqueue(new Callback<List<CheckinResponse>>() {
            @Override
            public void onResponse(Call<List<CheckinResponse>> call, Response<List<CheckinResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SharedPreferences prefs = context.getSharedPreferences("MayaAppPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    for (CheckinResponse c : response.body()) {
                        if (c.isConcluido()) {
                            editor.putBoolean("CONCLUIDO_" + c.getExercicioPrescritoId(), true);
                        }
                    }
                    editor.apply();
                }
                irParaMain();
            }
            @Override public void onFailure(Call<List<CheckinResponse>> call, Throwable t) {
                irParaMain();
            }
        });
    }

    private void irParaMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}