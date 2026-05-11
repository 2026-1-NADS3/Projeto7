package com.example.aplicativo_maya;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "MayaAppPrefs";
    // Chaves
    private static final String KEY_TOKEN         = "JWT_TOKEN";
    private static final String KEY_PACIENTE_ID   = "PACIENTE_ID";
    private static final String KEY_PACIENTE_NOME = "PACIENTE_NOME";
    private static final String KEY_PACIENTE_EMAIL = "PACIENTE_EMAIL";
    private static final String KEY_LOGADO        = "USUARIO_LOGADO";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void salvarSessao(String token, long pacienteId, String nome, String email) {
        editor.putBoolean(KEY_LOGADO, true);
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_PACIENTE_ID, String.valueOf(pacienteId));
        editor.putString(KEY_PACIENTE_NOME, nome != null ? nome : "");
        editor.putString(KEY_PACIENTE_EMAIL, email != null ? email : "");
        editor.apply();
    }

    public boolean estaLogado() {
        return prefs.getBoolean(KEY_LOGADO, false)
                && prefs.getString(KEY_TOKEN, null) != null;
    }

    public String getToken()        { return prefs.getString(KEY_TOKEN, ""); }
    public String getPacienteId()   { return prefs.getString(KEY_PACIENTE_ID, ""); }
    public String getPacienteNome() { return prefs.getString(KEY_PACIENTE_NOME, ""); }
    public String getPacienteEmail(){ return prefs.getString(KEY_PACIENTE_EMAIL, ""); }

    public void encerrarSessao() {
        editor.clear();
        editor.apply();
    }
}