package com.example.aplicativo_maya.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MayaRpgApi {
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("/api/paciente/agendamentos")
    Call<List<AgendamentoResponse>> getAgendamentos(@Header("Authorization") String token);

    @GET("/api/paciente/checkins")
    Call<List<CheckinResponse>> getMeusCheckins(@Header("Authorization") String token);
}