package com.example.aplicativo_maya.api;

public class ErrorResponse {
    private int status;
    private String error;
    private String messagem; // Mantendo o nome conforme o seu Swagger ("messagem")
    private String timeStamp;

    // Getters
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessagem() { return messagem; }
    public String getTimeStamp() { return timeStamp; }
}