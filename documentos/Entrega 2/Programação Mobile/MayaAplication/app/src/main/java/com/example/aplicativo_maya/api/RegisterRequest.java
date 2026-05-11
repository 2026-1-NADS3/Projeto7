package com.example.aplicativo_maya.api;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String cpf;
    private String telefone;
    private String dataNascimento;

    public RegisterRequest(String name, String email, String password, String cpf, String telefone, String dataNascimento) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.cpf = cpf;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
    }
}