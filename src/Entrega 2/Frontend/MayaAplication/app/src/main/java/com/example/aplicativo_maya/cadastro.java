package com.example.aplicativo_maya;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicativo_maya.api.AuthResponse;
import com.example.aplicativo_maya.api.MayaRpgApi;
import com.example.aplicativo_maya.api.RegisterRequest;
import com.example.aplicativo_maya.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class cadastro extends AppCompatActivity {

    private EditText txbNameCad, txbEmailCad, txbSenhaCad, txbConfirmaSenhaCad, txbCPFCad, txbTelefoneCad, txbDateCad;
    private Button btnCadastrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        txbNameCad = findViewById(R.id.txbNameCad);
        txbEmailCad = findViewById(R.id.txbEmailCad);
        txbSenhaCad = findViewById(R.id.txbSenhaCad);
        txbConfirmaSenhaCad = findViewById(R.id.txbConfirmaSenhaCad);
        txbCPFCad = findViewById(R.id.txbCPFCad);
        txbTelefoneCad = findViewById(R.id.txbTelefoneCad);
        txbDateCad = findViewById(R.id.txbDateCad);
        btnCadastrar = findViewById(R.id.btnCadastrar);

        txbCPFCad.addTextChangedListener(aplicarMascara("###.###.###-##", txbCPFCad));
        txbTelefoneCad.addTextChangedListener(aplicarMascara("(##) #####-####", txbTelefoneCad));
        txbDateCad.addTextChangedListener(aplicarMascara("##/##/####", txbDateCad));

        btnCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realizarCadastro();
            }
        });
    }

    private void realizarCadastro() {
        String nome = txbNameCad.getText().toString().trim();
        String email = txbEmailCad.getText().toString().trim();
        String senha = txbSenhaCad.getText().toString().trim();
        String confirmaSenha = txbConfirmaSenhaCad.getText().toString().trim();
        String cpf = txbCPFCad.getText().toString().trim();
        String telefone = txbTelefoneCad.getText().toString().trim();
        String dataNasc = txbDateCad.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || dataNasc.isEmpty() || cpf.isEmpty() || telefone.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show();
            return;
        }

        // VERIFICAÇÃO DE SENHA
        if (!senha.equals(confirmaSenha)) {
            Toast.makeText(this, "As senhas não coincidem!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dataNasc.contains("/")) {
            String[] partes = dataNasc.split("/");
            if (partes.length == 3) {
                dataNasc = partes[2] + "-" + partes[1] + "-" + partes[0];
            }
        }

        btnCadastrar.setEnabled(false);
        btnCadastrar.setText("Aguarde...");

        MayaRpgApi api = RetrofitClient.getInstance().create(MayaRpgApi.class);
        RegisterRequest request = new RegisterRequest(nome, email, senha, cpf, telefone, dataNasc);

        api.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnCadastrar.setEnabled(true);
                btnCadastrar.setText("Cadastrar");

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(cadastro.this, "Cadastro realizado! Faça seu login.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(cadastro.this, Login_Maya.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(cadastro.this, "Falha no cadastro. Verifique essa conta ja existe", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnCadastrar.setEnabled(true);
                btnCadastrar.setText("Cadastrar");
                Toast.makeText(cadastro.this, "Erro de conexão: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private TextWatcher aplicarMascara(final String mask, final EditText editText) {
        return new TextWatcher() {
            boolean isUpdating;
            String old = "";

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remover não numeros
                String str = s.toString().replaceAll("[^0-9]*", "");
                String mascara = "";

                if (isUpdating) {
                    old = str;
                    isUpdating = false;
                    return;
                }

                int i = 0;
                for (char m : mask.toCharArray()) {
                    if (m != '#' && str.length() > old.length()) {
                        mascara += m;
                        continue;
                    }
                    try {
                        mascara += str.charAt(i);
                    } catch (Exception e) {
                        break;
                    }
                    i++;
                }

                isUpdating = true;
                editText.setText(mascara);
                editText.setSelection(mascara.length());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }
}